(ns kanopi.controller.handlers.request
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]

            [kanopi.aether.core :as aether]
            [kanopi.controller.history :as history]
            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]

            [kanopi.util.core :as util]
            ))

(defmulti local-request-handler
  (fn [_ _ _ msg]
    (info msg)
    (get msg :verb)))

;; TODO: should this be in the response namespace? is there anything
;; to be done here but fwd it?
;; I could just pass it on, keeping the noun the same or transforming
;; it into something more useful or annotate it.
;; OR
;; I could do the work of matching inputs to routes here, instead of
;; passing the history component around via shared state.
(defmethod local-request-handler :spa/navigate
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
    (cond
     (= :datum handler)
     (let [datum-id (util/read-entity-id (get-in msg [:noun :route-params :id]))]
       (->> (msg/get-datum datum-id)
            (aether/send! aether))))))

(defmethod local-request-handler :spa/switch-team
  [aether history app-state {team-id :noun :as msg}]
  (let [user' (update @app-state :user
                      (fn [user]
                        (if-let [team' (->> (get user :teams)
                                            (filter #(= (:team/id %) team-id))
                                            (first))]
                          (assoc user :current-team team')
                          user)))]
    (->> (msg/switch-team-success user')
         (aether/send! aether))))

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
            ent))))

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

(defmethod local-request-handler :spa.navigate/search
  [aether history app-state msg]
  (let [{:keys [query-string entity-type]} (get msg :noun)
        results (local-fulltext-search @app-state query-string entity-type)
        ]
    (->> (msg/navigate-search-success query-string results)
         (aether/send! aether))))

(defn- current-datum [props]
  (get-in props [:datum :datum :db/id]))

(defn- lookup-id
  ([props id]
   (lookup-id props 0 id))
  ([props depth id]
   ;; FIXME: there is a correct depth cut-off. I don't know if this is
   ;; it. I'm not thinking too clearly right now.
   (if (> depth 10)
     id
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
                  {})))))

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
  "Data is stored as flat maps locally and on server, but to simplify
  datum component model we must nest entities as follows:
  datum -> facts -> attributes (literals or datums)
  -> values     (literals or datums)"
  [props datum-id]
  {:pre [(or (integer? datum-id) (string? datum-id))]}
  (let [context (context-datums (-> props :cache vals) datum-id)
        similar (similar-datums (-> props :cache vals) datum-id)
        datum   (lookup-id props datum-id)]
    (hash-map
     :context-datums [(lookup-id props -1008)]
     :datum datum
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

(defn- swap-nested-entity-in-fact-part
  [fact-part {:keys [fact new-entities] :as m}]
  (let [new-ent-id (util/next-id)
        new-ent (assoc (get fact fact-part)
                        :db/id new-ent-id)]
    (-> m
        (assoc-in [:fact fact-part] new-ent-id)
        (update :new-entities #(conj % new-ent)))))

(defn- parse-input-fact
  "
  1 - ensure fact has a :db/id
  2 - replace nested attr/value with refs and build new entities
  "
  [datum {:keys [fact/attribute fact/value] :as fact}]
  (let [is-new-fact (new-ent? fact)
        is-new-referenced-attribute (new-ent? attribute)
        is-new-referenced-value     (new-ent? value)

        {fact' :fact, new-entities :new-entities}
        (cond-> {:fact fact, :new-entities []}
          is-new-fact
          (assoc-in [:fact :db/id] (util/next-id))

          is-new-referenced-attribute
          ((partial swap-nested-entity-in-fact-part :fact/attribute))

          is-new-referenced-value
          ((partial swap-nested-entity-in-fact-part :fact/value)))

        datum' (if is-new-fact
                 (update datum :datum/fact #(conj % fact'))
                 (update datum :datum/fact
                         (fn [facts]
                           (->> facts
                                (remove #(= (get fact' :db/id) (:db/id %)))
                                (cons fact')))))
        ]
    (hash-map :datum datum'
              :new-entities new-entities)))

(defmethod local-request-handler :datum/create
  [aether history app-state msg]
  (let [dtm {:datum/label (get-in msg [:noun :label])
             :datum/team  (get-in app-state [:user :current-team :db/id])
             :db/id       (util/random-uuid)}
        st' (assoc-in @app-state [:cache (get dtm :db/id)] dtm)
        user-datum (build-datum-data st' (get dtm :db/id))
        ]
    (->> (msg/create-datum-success user-datum)
         (aether/send! aether))))

(defn- handle-fact-add-or-update
  [app-state msg]
  (let [datum nil
        {:keys [datum new-entities] :as parsed-input}
        (parse-input-fact datum (get-in msg [:noun :fact])) ]
    parsed-input))

(defmethod local-request-handler :datum.fact/add
  [aether history app-state msg]
  (let [{:keys [datum new-entities]} (handle-fact-add-or-update app-state msg)]
    (->> (msg/add-fact-success datum new-entities)
         (aether/send! aether))))

(defmethod local-request-handler :datum.fact/update
  [aether history app-state msg]
  (let [{:keys [datum new-entities]} (handle-fact-add-or-update app-state msg)]
    (->> (msg/update-fact-success datum new-entities)
         (aether/send! aether))))

(defmethod local-request-handler :datum.label/update
  [aether history app-state msg]
  (let [datum-id (get-in msg [:noun :existing-entity :db/id])
        new-label (get-in msg [:noun :new-label])
        datum    (get-in app-state [:cache datum-id])
        datum'   (assoc datum :datum/label new-label)]
    (->> (msg/update-datum-label-success datum')
         (aether/send! aether))))

(defmethod local-request-handler :datum/get
  [aether history app-state msg]
  (let [user-datum (build-datum-data app-state (get msg :noun))]
    (->> (msg/get-datum-success user-datum)
         (aether/send! aether))))

