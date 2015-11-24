(ns kanopi.aether.spout
  "Queued listener with n-parallelism, fifo semantics, duplicate
  request elimination, exponential back-off worker throttling and
  failed request logging.

  Initial use-case is controlling transport of messages from the Ether
  to an external web service.

  Future use-cases include anything where time-consuming work must be
  done per message -- and thus requires a controlled, potentially
  parallel, priority-aware mechanism for performing said work.
  "
  (:require-macros [cljs.core.async.macros :refer (go go-loop)])
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.aether.core :as aether]
            [ajax.core :as http]
            [clojure.set]
            [cljs-uuid-utils.core :refer (make-random-uuid)]
            [cljs-time.core :as time]
            [cljs-time.coerce :as time-coerce]
            [com.stuartsierra.component :as component]
            [cljs.core.async :as async :refer (<! >!)]))

(defn k-comparator [k]
  (fn [x y]
    (< (get x k) (get y k))))

(defn- new-request-queue
  "Sorted set as queue.

  I have not figured out exactly when a request is duplicating another
  request -- same uri but different handler fn, should both run? -- so
  I'm not using a sorted-map and I'm leaving the user to figure out
  how to identify duplicate requests."
  ([]
   (new-request-queue :timestamp))
  ([k]
   (atom
    (sorted-set-by (k-comparator k))
    :validator (fn [new-value]
                 (set? new-value)))))

(defn dequeue-set! [s]
  (let [itm (first @s)]
    ;; NOTE: i have no idea how/why this becomes a map when shutting
    ;; down the spout
    (swap! s #(disj % itm))
    itm))

(defn enqueue-set!
  ([s itm]
   (enqueue-set! s itm (constantly nil)))
  ([s itm dupe-fn]
   (info itm)
   (swap! s (fn [the-set]
              (if (some dupe-fn the-set)
                the-set
                (conj the-set itm))))))

(defn- parse-response-handler
  [{:keys [response-method response-xform]
    :or {response-xform identity}
    :as req} aether]
  (cond
   (nil? response-method)
   (assoc req :handler (constantly nil))

   (= response-method :log)
   (assoc req :handler (comp #(info %) response-xform))

   (= response-method :aether)
   (assoc req :handler (comp (partial aether/send! aether) response-xform))

   (fn? response-method)
   (assoc req :handler (comp response-method response-xform))

   :default
   (assoc req :handler (constantly nil))
   ))

(defn- parse-error-handler
  [{:keys [error-method error-xform]
    :or {error-xform identity}
    :as req} aether]
  (cond
   (nil? error-method)
   (assoc req :error-handler (constantly nil))

   (= error-method :log)
   (assoc req :error-handler (comp println error-xform))

   (= error-method :aether)
   (assoc req :error-handler (comp (partial aether/send! aether) error-xform))

   (fn? error-method)
   (assoc req :error-handler (comp error-method error-xform))

   :default
   (assoc req :error-handler (constantly nil))
   ))

(defn- parse-handlers
  "Handlers are provided"
  [req aether]
  (-> req (parse-response-handler aether) (parse-error-handler aether)))

(defn- wrap-handlers
  "Wrap success and error handlers to continue HTTPWorker's
  lifecycle after the given request is completed.

  NOTE: be careful not to send more than 1 message to recur-ch"
  ([req recur-ch]
   (wrap-handlers req recur-ch (constantly nil)))
  ([req recur-ch error-fn]
   (-> req
       (update :handler
               (fn [handler]
                 (fn [response]
                   (when handler
                     (handler response))
                   (async/put! recur-ch response))))

       (update :error-handler
               (fn [handler]
                 (fn [error]
                   (when handler
                     (handler error))
                   (async/put! recur-ch error)
                   (error-fn error))))
       )))

(defrecord HTTPWorker [id aether kill-ch notify-ch queue]
  component/Lifecycle
  (start [this]
    #_(debug id "start worker")
    (assert (not (nil? queue)))
    (let [kill-ch  (async/chan 100)
          recur-ch (async/chan 100)]

      (go (loop [[v ch] nil]
            ;; Stay alive?
            (when (not= ch kill-ch)

              (if-let [next-item (dequeue-set! queue)]

                ;; ACTIVE Recur Branch
                (let [req-fn    (case (:method next-item)
                                  :get  http/GET
                                  :put  http/PUT
                                  :post http/POST)
                      req-map   (-> next-item
                                    (parse-handlers aether)
                                    (wrap-handlers recur-ch)
                                    (select-keys [
                                                  :headers
                                                  :params :body
                                                  :handler :error-handler
                                                  :format :response-format
                                                  ]))]
                  #_(debug id "ACTIVE")
                  (info req-map)
                  (req-fn (:uri next-item) req-map)
                  (<! recur-ch)
                  (recur (async/alts! [kill-ch notify-ch])))

                ;; IDLE Recur Branch
                (let []
                  #_(debug id "IDLE")
                  (recur (async/alts! [kill-ch notify-ch])))))))

      (assoc this :kill-ch kill-ch)))

  (stop [this]
    #_(debug id "stop worker")
    (async/put! kill-ch :kill)
    (assoc this :kill-ch nil)))

(defn new-http-worker
  ([aether queue notify-ch]
   (map->HTTPWorker
    {:aether    aether
     :notify-ch notify-ch
     :id        (make-random-uuid)
     :queue     queue}
    )))

(defn- watch-and-notify
  ([a]
   (watch-and-notify a (async/chan 100)))
  ([a ch]
   (add-watch a nil (fn [key atom old-state new-state]
                      (when-let [diff (not-empty (clojure.set/difference new-state old-state))]
                        (async/put! ch diff))))
   ch))

;; Spouts act like pipelines where the receiving end is not a
;; core.async channel.
;;
;; Required configuration parameters:
;; - n-parallelism
;; - priority-fn
;; - dimension
;; - value
;; - xform
;;   - NOTE: all response handling is defined in the request map,
;;           which is produced by this xform fn.
;; - logfn
(defrecord HTTPSpout [config queue notify-ch workers kill-ch aether app-state]
  component/Lifecycle
  (start [this]
    (let [{:keys [n-parallelism priority-fn
                  dimension     value
                  xform         logfn]
           :or   {xform       identity
                  logfn       (constantly nil)
                  priority-fn (constantly 0)
                  n           3}}
          config
          ;_         (info "start spout" dimension value)
          q         (new-request-queue priority-fn)
          notify-ch (watch-and-notify q)
          workers   (doall (repeatedly n-parallelism
                                       #(component/start (new-http-worker aether q notify-ch))))
          kill-ch   (aether/listen* (get aether :aether)
                                    dimension value
                                    {:handlerfn
                                     (fn [req]
                                       (let [req' (xform req)]
                                         (enqueue-set! q req' #(= (:uri %) (:uri req')))))
                                     :logfn logfn
                                     })
          ]
      (assoc this
             :queue q
             :notify-ch notify-ch
             :workers workers
             :kill-ch kill-ch)))

  (stop [this]
    #_(info "stop spout" (get config :dimension) (get config :value))
    (doall (map component/stop workers))
    (async/put! kill-ch :kill!)
    (assoc this
           :queue     (swap! queue #(set {}))
           :notify-ch (async/close! notify-ch)
           :kill-ch   (async/close! kill-ch)
           :workers   (doall (map component/stop workers))))
  )

(defn new-http-spout
  "Handle all default configuration values here.

  Required args: dimension, value.
  Optional args: n, priority-fn, logfn.
  "
  ([dimension value config]
   (let [{:or {xform       identity
               logfn       (constantly nil)
               priority-fn (constantly 0)
               n           3}
          :keys [n priority-fn xform logfn]}
         config]
     (map->HTTPSpout
      {:config {:n-parallelism n
                :dimension dimension
                :value value
                :priority-fn priority-fn
                :xform xform
                :logfn logfn}
       }))))

(defn spout!
  "See new-http-spout for arg guidance."
  ([& args]
   (component/start (apply new-http-spout args))))
