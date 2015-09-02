;; ## An application's message-passing medium.
;; ### Because cities aren't made of trees. (they're made of graphs!)
;;
;; Decomplects message passing from the programming language,
;; computing process, physical environment and code structure.
;;
;; There is a single publisher channel, :publisher.
;; For each dimension supplied to mk-ether, a publication
;; is constructed whose topic-fn is the dimension keyword.
;;
;; Suggested ethereum:
;;
;; 1. [:signal]
;; 2. [:verb :payload]
;; 3. [:subject :predicate :object] (RDF)
;;     - produces a map with keys:
;;       [:publisher
;;        :subject-publication
;;        :predicate-publication
;;        :object-publication]
;; 4. [:entity :attribute :value :transaction] (Datomic's Data Model)
;;
;; What do components want to do?
;;
;; Consume, Transform and Forward outside the Ether, Transform and
;; Release back into the Ether,
;;
;; The use-case for Om:
;;
;; - lift api communications and data gathering out of the components.
;;   they should just ask for what they need in a very generic way.
;; - broadcast app-state updates to whoever cares, without knowing that
;;   ahead of time
;;
;; The channels are provided to Om Components as global shared state.
;;
;; Any component can use the publisher to send a message anywhere,
;; including itself.
;;
;; Some guidelines for message grammar...
;;
;; - Messages should say nothing about implementation.
;; - Messages should never drop below the grammar most natural
;; to the sender.
;; - Message recipients should always exist in the grammar most
;; natural to them. Ideally, implementation logic and business
;; logic can be layered with core.async messages in between.
;;
;; Ether feels like a component that is started.
;;
;; Ether consists of a publisher, a map of publications, spouts and
;; funnels.
;;
;; Spouts and funnels are components implementing protocols.
;;
;; Spouts are listeners with their own backpressure
;; semantics independent of the ether. Spouts may have transducers.
;;
;; Funnels mix input from other channels and transduce the input into
;; messages, which are then pushed onto the publisher.
;;
;;
;; Listeners are ripe for improvement.
;;
;; I want to do
;; (ether/listen! owner
;;  '{:subject [?ns ?iri], :predicate [:swap!], :object ?object}
;;  (fn [{:keys [?ns ?iri ?object]} msg]
;;    ))
;;
;; If I want a more flexible listen! interface I must make the
;; publication map mutable. Listeners must be able to create a new
;; publication with its custom topicfn.
;;
(ns kanopi.ether.core
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go]])
  (:require [cljs.core.async :as async]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [om.core :as om]
            [quile.component :as component]))

(defn- deref-cursor
  "Transducer to let xform fns not worry about dereferencing cursors."
  [x]
  (if (om/cursor? x)
    (deref x) x))

(defn- mk-tap
  "TODO: think carefully about the tap's buffer"
  [mult _]
  (async/tap mult (async/chan 100)))

(defn- publication-keyword [dimension-key]
  (-> dimension-key name (str "-publication") keyword))

(defn mk-ether
  "Both construct and start ether.

  Can I construct publications for every permutation of keywords?
  If so, I'd need to name them in a predictable manner.

  It would also be good to create a function to find the publication
  for a sequence of keywords."
  [& dimension-keys]
  (let [publisher    (async/chan 100)
        pub-mult     (async/mult publisher)
        taps         (map (partial mk-tap pub-mult) dimension-keys)
        pub-keys     (map publication-keyword dimension-keys)
        publications (map async/pub taps dimension-keys)]
    (merge {:publisher publisher}
           (zipmap pub-keys publications))))

(defn- get-publication [ether dimension]
  (->> dimension (publication-keyword) (get ether)))

(defn listen*
  ([ether dimension value opts]
   (let [{:keys [kill-ch handlerfn logfn]
          :or {kill-ch (async/chan)
               logfn     (constantly nil)}}
         opts
         listener (async/chan 100)
         publication (get-publication ether dimension)]
     (assert handlerfn "Handler function is required.")
     (async/sub publication value listener)
     (go (loop [[v ch] nil]
           (if (= ch kill-ch)
             (do
              (async/unsub publication value listener)
              (async/close! listener))
             (do
              (when v
                (logfn v)
                (handlerfn v))
              (recur (async/alts! [listener kill-ch]))))))
     kill-ch)))

(defn feed!
  "NOTE: experimental."
  ([ether dimension value recipient-ch]
   (listen* ether dimension value
            {:handlerfn (partial async/put! recipient-ch)
             :logfn (constantly nil)})))

(defn listen!
  "Pass messages matching the supplied dimension and value to a
  callback function.
  "
  ([owner dimension value handlerfn]
   (listen! owner dimension value handlerfn (constantly nil)))
  ([owner dimension value handlerfn logfn]
   (let [kill-ch (listen* (om/get-shared owner :ether)
                          dimension value
                          {:handlerfn handlerfn
                           :logfn     logfn})]
     (om/set-state! owner [::kill-ch value] kill-ch))))

(defn trigger-kill-chans!
  ([owner]
   (->> (om/get-state owner ::kill-ch)
        (vals)
        (map #(async/put! % :kill))
        (doall))))

(defn stop-spouts!
  ([owner]
   (->> (om/get-state owner :spouts)
        (vals)
        (map component/stop)
        (doall))))

(defn shutdown-component! [owner]
  ((juxt trigger-kill-chans!
         stop-spouts!)
   owner))

(def stop-listening! shutdown-component!)

(defrecord Ether [config ether]
  component/Lifecycle
  (start [this]
    (if ether
      this
      (let [ethr (apply mk-ether (:dimensions config))]
        (info "start ether" (:dimensions config))
        (assoc this :ether ethr))))

  (stop [this]
    (if-not ether
      this
      (do
       (info "stop ether")
       ;; TODO: kill ether
       (assoc this :ether nil)))))

(defn new-ether [config]
  (map->Ether {:config config}))
