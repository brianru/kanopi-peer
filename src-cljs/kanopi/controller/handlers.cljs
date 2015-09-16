(ns kanopi.controller.handlers
  "All app-state transformations are defined here.
  
  TODO: refactor to work with om cursors instead of atoms."
  (:require [om.core :as om]
            [kanopi.util.core :as util]
            [kanopi.model.schema :as schema]
            [cljs-uuid-utils.core :refer (make-random-uuid)]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(defmulti local-event-handler
  (fn [app-state msg]
    (get msg :verb))
  :default
  :log)

(defmethod local-event-handler :log
  [app-state msg]
  (info msg))

(defn- current-thunk [props]
  (get-in props [:thunk :thunk :db/id]))

(defn- lookup-id
  ([props id]
   (->> (get-in props [:cache id])
        (reduce (fn [acc [k v]]
                  (cond
                   (= k :thunk/fact)
                   (assoc acc k (set (map (partial lookup-id props) v)))
                   (= k :fact/attribute)
                   (assoc acc k (set (map (partial lookup-id props) v)))
                   (= k :fact/value)
                   (assoc acc k (set (map (partial lookup-id props) v)))

                   :default
                   (assoc acc k v)))
                {})
        )))

(def placeholder-fact
  {:db/id nil
   :fact/attribute #{{:db/id nil}}
   :fact/value     #{{:db/id nil}}})

(defn- build-thunk-data
  "
  Data is stored as flat maps locally and on server, but to simplify
  thunk component model we must nest entities as follows:
  thunk -> facts -> attributes (literals or thunks)
  -> values     (literals or thunks)"
  [props thunk-id]
  {:pre [(integer? thunk-id)]}
  (let [thunk (lookup-id props thunk-id)]
    (hash-map
     :context-thunks #{(lookup-id props -1008)
                       }
     :thunk (update thunk :thunk/fact #(conj % placeholder-fact))
     :similar-thunks #{(lookup-id props -1016)
                       })))

(defn- navigate-to-thunk [props msg]
  (let [thunk-id (cljs.reader/read-string (get-in msg [:noun :route-params :id]))
        thunk' (build-thunk-data props thunk-id)]
    (assoc props :thunk thunk')))

(defn- ensure-current-thunk-is-updated [props edited-ent-id]
  (if (= edited-ent-id (current-thunk props))
    (let []
      (assoc props :thunk (build-thunk-data props edited-ent-id)))
    props))

(defmethod local-event-handler :change-entity-type
  [app-state msg]
  )

(defmethod local-event-handler :update-thunk-label
  [app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [ent-id (get-in msg [:noun :existing-entity :db/id])
                        label' (get-in msg [:noun :new-label])]
                    (-> app-state
                        (assoc-in [:cache ent-id :thunk/label] label')
                        (ensure-current-thunk-is-updated ent-id))))))

(defmethod local-event-handler :update-fact
  [app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [thunk-id (get-in msg [:noun :thunk-id])
                        fact     (get-in msg [:noun :fact])
                        [is-new-fact fact'] (if (:db/id fact)
                                              [false fact]
                                              [true  (assoc fact :db/id (make-random-uuid))])
                        ]
                    (cond-> (assoc-in app-state [:cache (:db/id fact')] fact')

                      is-new-fact
                      (update-in [:cache thunk-id :thunk/fact] #(conj % (:db/id fact')))
                      
                      true
                      (ensure-current-thunk-is-updated thunk-id))
                    ))))

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
  [app-state msg]
  (om/transact! app-state
         (fn [app-state]
           (let [{:keys [query-string entity-type]} (get msg :noun)
                 results (local-fulltext-search app-state query-string entity-type)]
             ;; wipes out map with each new search.
             ;; not using this data structure very well
             (assoc-in app-state [:search-results] {query-string results})))))



(defmethod local-event-handler :navigate
  [app-state msg]
  (let [handler (get-in msg [:noun :handler])]
    (om/transact! app-state
           (fn [app-state]
             (cond-> app-state
               true
               (assoc :page (get msg :noun))

               ;; TODO: implement user lifecycle in spa
               (= :login handler)
               identity

               (= :logout handler)
               (assoc :user nil)

               (= :register handler)
               (assoc :user nil)

               (= :thunk handler)
               (navigate-to-thunk msg)

               (not= :thunk handler)
               (assoc :thunk {})

               ))))
  )
