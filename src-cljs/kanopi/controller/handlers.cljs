(ns kanopi.controller.handlers
  "All app-state transformations are defined here.

  TODO: refactor to work with om cursors instead of atoms."
  (:require [om.core :as om]
            [kanopi.util.core :as util]
            [kanopi.aether.core :as aether]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]
            [kanopi.controller.history :as history]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(defmulti local-event-handler
  (fn [aether history app-state msg]
    (println "local-event-handler" msg)
    (get msg :verb))
  :default
  :log)

(defmethod local-event-handler :log
  [aether history app-state msg]
  (info msg)
  msg)

(defn- current-datum [props]
  (get-in props [:datum :datum :db/id]))

(defn- lookup-id
  ([props id]
   (lookup-id props 0 id))
  ([props depth id]
   ;; FIXME: there is a correct depth cut-off. I don't know if this is
   ;; it. I'm not thinking too clearly right now.
   (if (> depth 10) id
     (->> (get-in props [:cache id])
          (reduce (fn [acc [k v]]
                    (cond
                     (= k :datum/fact)
                     (assoc acc k (mapv (partial lookup-id props (inc depth)) v))
                     (= k :fact/attribute)
                     (assoc acc k (mapv (partial lookup-id props (inc depth)) v))
                     (= k :fact/value)
                     (assoc acc k (mapv (partial lookup-id props (inc depth)) v))

                     :default
                     (assoc acc k v)))
                  {})
          ))
   ))

(defn- references-datum? [props base-id ent]
  (->> ent
       :db/id
       (lookup-id props)
       :datum/fact))

(defn- context-datums
  "Find all datums in `data` which reference the base-id."
  [props base-id]
  (let [data    (-> props :cache (vals))
        ref-ids (->> data
                     (filter schema/datum?)
                     (filter (partial references-datum? props base-id))
                     (map :db/id)
                     (take 9)
                     )]
    ))
(defn- similar-datums [data base-id]
  )

(def placeholder-fact
  {:db/id nil
   :fact/attribute [{:db/id nil}]
   :fact/value     [{:db/id nil}]})

(defn- build-datum-data
  "
  Data is stored as flat maps locally and on server, but to simplify
  datum component model we must nest entities as follows:
  datum -> facts -> attributes (literals or datums)
  -> values     (literals or datums)"
  [props datum-id]
  {:pre [(integer? datum-id)]}
  (let [context (context-datums (-> props :cache vals) datum-id)
        similar (similar-datums (-> props :cache vals) datum-id)
        datum (lookup-id props datum-id)]
    (hash-map
     :context-datums [(lookup-id props -1008)]
     :datum (update datum :datum/fact #(vec (conj % placeholder-fact)))
     :similar-datums [(lookup-id props -1016)])))

(defn- ensure-current-datum-is-updated [props edited-ent-id]
  (if (= edited-ent-id (current-datum props))
    (let [datum' (build-datum-data props edited-ent-id)]
      (assoc props :datum datum'))
    props))

(defn new-ent? [ent]
  (cond
   (map? ent)
   (nil? (:db/id ent))
   
   (integer? ent)
   false
   
   :default
   true))

(defn- prepare-fact [fact]
  (cond-> {:is-new-fact false
           :is-new-referenced-attribute false
           :is-new-referenced-value false
           :fact fact}

    (new-ent? fact)
    ((fn [{:keys [fact] :as existing}]
       (let [fact' (assoc fact :db/id (util/next-id))]
         (assoc existing
                :is-new-fact true
                :fact fact'))))
    
    (new-ent? (-> fact :fact/attribute first))
    ((fn [{:keys [fact] :as existing}]
       (let [attr' (-> fact :fact/attribute first (assoc :db/id (util/next-id)))]
         (assoc existing
                :new-referenced-attribute attr'
                :fact (assoc fact :fact/attribute [(get attr' :db/id)])))))

    (new-ent? (-> fact :fact/value first))
    ((fn [{:keys [fact] :as existing}]
       (let [value' (-> fact :fact/value first (assoc :db/id (util/next-id)))]
         (assoc existing
                :new-referenced-value value'
                :fact (assoc fact :fact/value [(get value' :db/id)])))))))

(defmethod local-event-handler :update-fact
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [datum-id (get-in msg [:noun :datum-id])
                        fact     (get-in msg [:noun :fact])

                        {fact' :fact
                         :keys [is-new-fact new-referenced-attribute new-referenced-value]
                         :as prepared-info}
                        (prepare-fact fact)

                        app-state'

                        (cond-> (assoc-in app-state [:cache (:db/id fact')] fact')

                          is-new-fact
                          (update-in [:cache datum-id :datum/fact] #(conj % (:db/id fact')))

                          is-new-fact
                          (assoc-in [:cache (:db/id fact')] fact')

                          new-referenced-attribute
                          (assoc-in [:cache (:db/id new-referenced-attribute)]
                                    new-referenced-attribute)

                          new-referenced-value
                          (assoc-in [:cache (:db/id new-referenced-value)]
                                    new-referenced-value)

                          true
                          (ensure-current-datum-is-updated datum-id))
                        ]
                    app-state'
                    ))))

(defmethod local-event-handler :update-datum-label
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [ent-id (get-in msg [:noun :existing-entity :db/id])
                        label' (get-in msg [:noun :new-label])]
                    (-> app-state
                        (assoc-in [:cache ent-id :datum/label] label')
                        (ensure-current-datum-is-updated ent-id))))))

;; TODO: implement.
(defmethod local-event-handler :update-datum-label-success
  [aether history app-state msg]
  )

;; TODO: implement.
(defmethod local-event-handler :update-datum-label-failure
  [aether history app-state msg]
  )

(defn- fuzzy-search-entity [q ent]
  (let [base-string (->> ent
                         ((juxt :datum/label :value/string))
                         (apply str)
                         (clojure.string/lower-case)
                         )
        query-string (clojure.string/lower-case q)
        match-string (re-find (re-pattern query-string) base-string)]
    (when-not (or (clojure.string/blank? base-string)
                  (clojure.string/blank? match-string))
      (list (/ (count base-string) (count match-string))
            ent)))
  )

(defn- matching-entity-type [tp ent]
  (if-not tp true
    (= tp (schema/describe-entity ent))))

(defn- local-fulltext-search
  "TODO: sort by match quality
  https://github.com/Yomguithereal/clj-fuzzy
  TODO: handle upper- vs lower-case better
  TODO: only show x many
  TODO: deal with empty q better
  "
  [app-state q tp]
  (let []
    (->> (get-in app-state [:cache])
         (vals)
         (filter (partial matching-entity-type tp))
         (map (partial fuzzy-search-entity q))
         (remove nil?)
         (sort-by first)
         (vec))))

(defmethod local-event-handler :search
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [{:keys [query-string entity-type]} (get msg :noun)
                        results (local-fulltext-search app-state query-string entity-type)]
                    ;; wipes out map with each new search.
                    ;; not using this data structure very well
                    (assoc-in app-state [:search-results] {query-string results})))))

;; TODO: when handled locally, shouldn't I follow the same code path
;; as performing action remotely? eg. send success/failure msgs?
;; OR, should I purposely not do this and have a clear distinction b/w
;; actions handled transactionally vs hypothetically
(defmethod local-event-handler :get-datum
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (->> (get msg :noun)
                       (build-datum-data app-state)
                       (assoc app-state :datum)))))

;; TODO: don't I also have to put this stuff in the cache?
(defmethod local-event-handler :get-datum-success
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (->> (get msg :noun)
                       (build-datum-data app-state)
                       (assoc app-state :datum)))))

(defmethod local-event-handler :navigate
  [aether history app-state msg]
  (let [handler (get-in msg [:noun :handler])]
    (om/transact! app-state
                  (fn [app-state]
                    (cond-> app-state
                      true
                      (assoc :page (get msg :noun))

                      (not= :datum handler)
                      (assoc :datum {})

                      )))
    (when (= :datum handler)
      (let [datum-id (cljs.reader/read-string (get-in msg [:noun :route-params :id]))]
        (->> (msg/get-datum datum-id)
             (aether/send! aether))))))

(defmethod local-event-handler :register-success
  [aether history app-state {:keys [noun] :as msg}]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user noun
                           :mode :authenticated
                           :datum {:context-datums []
                                   :datum {}
                                   :similar-datums []}
                           :cache {})))
    (history/navigate-to! history :home)
    (->> (msg/initialize-client-state noun)
         (aether/send! aether))))

;; TODO: implement.
;; NOTE: example implementation. think about it more.
(defmethod local-event-handler :register-failure
  [aether history app-state msg]
  (let []
    (om/transact! app-state :error-messages
                  (fn [msgs]
                    (conj msgs {:type :register-failure
                                :msg  msg})))))

;; TODO: this must get a lot more data. we must re-initialize
;; app-state with this users' data.
(defmethod local-event-handler :login-success
  [aether history app-state {:keys [noun] :as msg}]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user noun
                           :mode :authenticated
                           :datum {:context-datums []
                                   :datum {}
                                   :similar-datums []}
                           :cache {})))
    (history/navigate-to! history :home)
    (->> (msg/initialize-client-state noun)
         (aether/send! aether))))

;; TODO: implement.
(defmethod local-event-handler :login-failure
  [aether history app-state msg]
  (let []
    ))

(defmethod local-event-handler :logout-success
  [aether history app-state msg]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user nil
                           :mode :demo)))
    (history/navigate-to! history :home)))

;; TODO: implement.
(defmethod local-event-handler :logout-failure
  [aether history app-state msg]
  (let []
    ))

(defmethod local-event-handler :initialize-client-state-success
  [aether history app-state msg]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (merge app-state (get msg :noun))))))

;; TODO: implement.
(defmethod local-event-handler :initialize-client-state-success
  [aether history app-state msg]
  )
