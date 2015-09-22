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
            [kanopi.aether.core :as aether]
            [ajax.core :as http]
            [cljs-uuid-utils.core :refer (make-random-uuid)]
            [cljs-time.core :as time]
            [cljs-time.coerce :as time-coerce]
            [quile.component :as component]
            [cljs.core.async :as async :refer (<! >!)]))

(defprotocol ISpout
  (enqueue [this itm])
  (dequeue [this]))

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
    (sorted-set-by (k-comparator k)))))

(defn dequeue-set! [s]
  (let [itm (first @s)]
    (swap! s #(disj % itm))
    itm))

(defn enqueue-set!
  ([s itm]
   (enqueue-set! s itm (constantly nil)))
  ([s itm dupe-fn]
   (swap! s (fn [s]
              (if (some dupe-fn s)
                s
                (conj s itm))))))

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

(defrecord HTTPWorker [id kill-ch notify-ch queue]
  component/Lifecycle
  (start [this]
    ;;(println id "start worker")
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
                      req-map   (-> (:request next-item)
                                    (wrap-handlers recur-ch))]
                  ;;(println id "ACTIVE")
                  (req-fn (:uri next-item) req-map)
                  (<! recur-ch)
                  (recur (async/alts! [kill-ch notify-ch])))

                ;; IDLE Recur Branch
                (let []
                  ;;(println id "IDLE")
                  (recur (async/alts! [kill-ch notify-ch])))))))

      (assoc this :kill-ch kill-ch)))

  (stop [this]
    ;;(println id "stop worker")
    (async/put! kill-ch :kill)
    (assoc this :kill-ch nil)))

(defn new-http-worker
  ([notify-ch]
   (new-http-worker (new-request-queue) notify-ch))
  ([queue notify-ch]
   (map->HTTPWorker
    {:notify-ch notify-ch
     :id (make-random-uuid)
     :queue queue}
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
(defrecord HTTPSpout [config queue notify-ch owner workers]
  component/Lifecycle
  (start [this]
    ;;(println "start spout" (clj->js (select-keys config [:dimension :value])))
    (let [{:keys [n-parallelism priority-fn
                  dimension     value
                  xform         logfn]}
          config
          q         (new-request-queue priority-fn)
          notify-ch (watch-and-notify q)
          workers   (doall (repeatedly n-parallelism
                                       #(component/start (new-http-worker q notify-ch))))
          this'     (assoc this :queue q :notify-ch notify-ch :workers workers)]

      (om/set-state! owner [:spouts [dimension value]] this')
      (aether/listen! owner dimension value
                     (fn [req] (enqueue this' (xform req)))
                     logfn)

      this'))

  (stop [this]
    ;;(println "stop spout")
    (assoc this
           :queue (swap! queue (constantly {}))
           :notify-ch (async/close! notify-ch)
           :workers (doall (map component/stop workers))))

  ISpout
  (enqueue [this request]
    (let [dupe-fn #(= (:uri %) (:uri request))]
      (enqueue-set! queue request dupe-fn)
      ;;(println "queue count: " (count @queue))
      ))
  (dequeue [_]
    (dequeue-set! queue)))

(defn new-http-spout
  "Handle all default configuration values here.

  Required args: owner, dimension, value.
  Optional args: n, priority-fn, logfn.
  "
  ([owner dimension value & args]
   (let [{:or {xform       identity
               logfn       (constantly nil)
               priority-fn :timestamp
               n           3}
          :keys [n priority-fn xform logfn]}
         (apply hash-map args)]
     (map->HTTPSpout
      {:config {:n-parallelism n
                :dimension dimension
                :value value
                :priority-fn priority-fn
                :xform xform
                :logfn logfn}
       :owner owner}))))

(defn spout!
  "See new-http-spout for arg guidance."
  ([& args]
   (component/start (apply new-http-spout args))))
