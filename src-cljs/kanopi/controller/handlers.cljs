(ns kanopi.controller.handlers
  "All app-state transformations are defined here.

  TODO: refactor to work with om cursors instead of atoms."
  (:require [om.core :as om]
            [kanopi.util.core :as util]
            [kanopi.aether.core :as aether]
            [kanopi.model.schema :as schema]
            [kanopi.controller.history :as history]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(defmulti local-event-handler
  (fn [aether history app-state msg]
    (println msg)
    (get msg :verb))
  :default
  :log)

(defmethod local-event-handler :log
  [aether history app-state msg]
  (info msg))

(defn- current-thunk [props]
  (get-in props [:thunk :thunk :db/id]))

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
                     (= k :thunk/fact)
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

(defn- references-thunk? [props base-id ent]
  (->> ent
       :db/id
       (lookup-id props)
       :thunk/fact))

(defn- context-thunks
  "Find all thunks in `data` which reference the base-id."
  [props base-id]
  (let [data    (-> props :cache (vals))
        ref-ids (->> data
                     (filter schema/thunk?)
                     (filter (partial references-thunk? props base-id))
                     (map :db/id)
                     (take 9)
                     )]
    ))
(defn- similar-thunks [data base-id]
  )

(def placeholder-fact
  {:db/id nil
   :fact/attribute [{:db/id nil}]
   :fact/value     [{:db/id nil}]})

(defn- build-thunk-data
  "
  Data is stored as flat maps locally and on server, but to simplify
  thunk component model we must nest entities as follows:
  thunk -> facts -> attributes (literals or thunks)
  -> values     (literals or thunks)"
  [props thunk-id]
  {:pre [(integer? thunk-id)]}
  (let [context (context-thunks (-> props :cache vals) thunk-id)
        similar (similar-thunks (-> props :cache vals) thunk-id)
        thunk (lookup-id props thunk-id)]
    (hash-map
     :context-thunks [(lookup-id props -1008)]
     :thunk (update thunk :thunk/fact #(vec (conj % placeholder-fact)))
     :similar-thunks [(lookup-id props -1016)])))

(defn- navigate-to-thunk [props msg]
  (let [thunk-id (cljs.reader/read-string (get-in msg [:noun :route-params :id]))
        thunk' (build-thunk-data props thunk-id)]
    (assoc props :thunk thunk')))

(defn- ensure-current-thunk-is-updated [props edited-ent-id]
  (if (= edited-ent-id (current-thunk props))
    (let [thunk' (build-thunk-data props edited-ent-id)]
      (assoc props :thunk thunk'))
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
                  (let [thunk-id (get-in msg [:noun :thunk-id])
                        fact     (get-in msg [:noun :fact])

                        {fact' :fact
                         :keys [is-new-fact new-referenced-attribute new-referenced-value]
                         :as prepared-info}
                        (prepare-fact fact)

                        app-state'

                        (cond-> (assoc-in app-state [:cache (:db/id fact')] fact')

                          is-new-fact
                          (update-in [:cache thunk-id :thunk/fact] #(conj % (:db/id fact')))

                          is-new-fact
                          (assoc-in [:cache (:db/id fact')] fact')

                          new-referenced-attribute
                          (assoc-in [:cache (:db/id new-referenced-attribute)]
                                    new-referenced-attribute)

                          new-referenced-value
                          (assoc-in [:cache (:db/id new-referenced-value)]
                                    new-referenced-value)

                          true
                          (ensure-current-thunk-is-updated thunk-id))
                        ]
                    app-state'
                    ))))

(defmethod local-event-handler :update-thunk-label
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [ent-id (get-in msg [:noun :existing-entity :db/id])
                        label' (get-in msg [:noun :new-label])]
                    (-> app-state
                        (assoc-in [:cache ent-id :thunk/label] label')
                        (ensure-current-thunk-is-updated ent-id))))))

(defn- fuzzy-search-entity [q ent]
  (let [base-string (->> ent
                         ((juxt :thunk/label :value/string))
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

(defmethod local-event-handler :navigate
  [aether history app-state msg]
  (let [handler (get-in msg [:noun :handler])]
    (om/transact! app-state
                  (fn [app-state]
                    (cond-> app-state
                      true
                      (assoc :page (get msg :noun))

                      (= :thunk handler)
                      (navigate-to-thunk msg)

                      (not= :thunk handler)
                      (assoc :thunk {})

                      )))))

(defmethod local-event-handler :register-success
  [aether history app-state msg]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user (get msg :noun)
                           :mode :authenticated)))
    (history/navigate-to! history :home)))

(defmethod local-event-handler :register-failure
  [aether history app-state msg]
  (let []
    ))

(defmethod local-event-handler :login-success
  [aether history app-state msg]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user (get msg :noun)
                           :mode :authenticated)))
    (history/navigate-to! history :home)
    ))

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

(defmethod local-event-handler :logout-failure
  [aether history app-state msg]
  (let []
    ))
