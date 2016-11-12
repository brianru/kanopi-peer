(ns kanopi.controller.handlers.request
  "TODO: refactor to return a collection of messages. Let the
  dispatcher handle them. These fns should be medium agnostic."
  (:require [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             (log trace debug info warn error fatal report)]
            [kanopi.model.message :as msg :refer (success-verb failure-verb)]
            [kanopi.model.schema :as schema]
            [kanopi.controller.handlers.request.search :as search]
            [kanopi.util.core :as util]))

(defmulti local-request-handler
  (fn [_ msg]
    (info msg)
    (get msg :verb)))

(defmethod local-request-handler :spa/navigate
  [_ msg]
  (let [{:keys [handler route-params] :as page'} (get-in msg [:noun])
        msgs (cond-> [(msg/navigate-success page')]
               (= :datum handler)
               (conj (msg/get-datum (util/read-entity-id (:id route-params))))

               (= :literal handler)
               (conj (msg/get-literal (util/read-entity-id (:id route-params)))))]
    (hash-map :messages msgs)))

(defmethod local-request-handler :spa/switch-team
  [app-state {team-id :noun :as msg}]
  (let [{user' :user}
        (update @app-state :user
                (fn [user]
                  (if-let [team' (->> (get user :teams)
                                      (filter (comp #{team-id} :team/id))
                                      (first))]
                    (assoc user :current-team team')
                    user)))]
    (hash-map :messages [(msg/switch-team-success user')])))

(defmethod local-request-handler :spa.navigate/search
  [app-state msg]
  (let [{:keys [query-string entity-type]} (get msg :noun)
        results (-> app-state (deref) :cache (vals)
                    (search/local-fulltext-search query-string entity-type))]
    (hash-map :messages [(msg/navigate-search-success query-string entity-type results)])))

(defn- current-datum [props]
  (get-in props [:datum :datum :db/id]))

(defmulti lookup-entity
  (fn [app-state id]
    (println "LOOKUP ENTITY" id (get-in app-state [:cache id]))
    (schema/describe-entity (get-in app-state [:cache id]))))

(defmethod lookup-entity :unknown
  [app-state id]
  (info "unknown-entity: " id)
  nil)

(defmethod lookup-entity :fact
  [app-state id]
  (when-let [{:keys [fact/attribute fact/value] :as normalized-fact}
             (get-in app-state [:cache id])]
    (assoc normalized-fact
           :fact/attribute
           (lookup-entity app-state attribute)

           :fact/value
           (lookup-entity app-state value)

           )))

(defmethod lookup-entity :team
  [app-state id]
  (when-let [normalized-team (get-in app-state [:cache id])]
    normalized-team))

(defmethod lookup-entity :literal [app-state id]
  (when-let [normalized-literal (get-in app-state [:cache id])]
    normalized-literal))

(defmethod lookup-entity :datum [app-state id]
  (when-let [{:keys [datum/fact datum/team] :as normalized-datum}
             (get-in app-state [:cache id])]
    (info "datum:" normalized-datum)
    (assoc normalized-datum
           :datum/fact
           (mapv (partial lookup-entity app-state) fact)
           
           :datum/team
           (lookup-entity app-state team)
           )))

(defn- references-datum? [props base-id ent]
  (->> ent
       :db/id
       (lookup-entity props)
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

;; FIXME: this does not work.
;; NOTE: or, the facts are not making their way to the cache and
;; that's actually important. that's probably it.
(defn- build-datum-data
  "Data is stored as flat maps locally and on server, but to simplify
  datum component model we must nest entities as follows:
  datum -> facts -> attributes (literals or datums)
  -> values     (literals or datums)"
  [props datum-id]
  {:pre [(or (integer? datum-id) (string? datum-id))]}
  (let [context (context-datums (-> props :cache vals) datum-id)
        similar (similar-datums (-> props :cache vals) datum-id)
        datum   (lookup-entity props datum-id)]
    (hash-map
     :context-datums []
     :datum datum
     :similar-datums [])))

(defn- build-literal-data
  ""
  [props literal-id]
  (let [literal (lookup-entity props literal-id)
        context (context-datums (-> props :cache vals) literal-id)]
    (hash-map
     :literal literal
     :context-datums context)))

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
        (assoc-in [:fact fact-part] new-ent)
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
    ;; NOTE: the facts make it here. but the datum cache may not have
    ;; the latest facts.
    (hash-map :datum datum'
              :new-entities new-entities)))

(defmethod local-request-handler :datum/create
  [app-state msg]
  (let [dtm {:datum/label (get-in msg [:noun :label])
             :datum/team  (get-in @app-state [:user :current-team])
             :db/id       (util/random-uuid)}
        user-datum (hash-map :context-datums []
                             :datum dtm
                             :similar-datums [])]
    (println "DATUM/CREATE HANDLER" (get-in @app-state [:user]))
    (hash-map :messages [(msg/create-datum-success user-datum)])))

(defn- handle-fact-add-or-update
  [app-state msg]
  (let [datum (get-in app-state [:datum :datum])
        fact  (get-in msg [:noun :fact])]
    (assert (= (:db/id datum) (get-in msg [:noun :datum-id]))
            "Can only add or update fact for current datum.")
    (parse-input-fact datum fact)))

(defmethod local-request-handler :datum.fact/add
  [app-state msg]
  (let [{:keys [datum new-entities]} (handle-fact-add-or-update app-state msg)]
    (hash-map :messages [(msg/add-fact-success datum new-entities)])))

(defmethod local-request-handler :datum.fact/update
  [app-state msg]
  (let [{:keys [datum new-entities]} (handle-fact-add-or-update app-state msg)]
    (hash-map :messages [(msg/update-fact-success datum new-entities)])))

(defmethod local-request-handler :datum.label/update
  [app-state msg]
  (let [datum-id (get-in msg [:noun :existing-entity :db/id])
        new-label (get-in msg [:noun :new-label])
        datum    (get-in app-state [:cache datum-id])
        datum'   (assoc datum :datum/label new-label)]
    (hash-map :messages [(msg/update-datum-label-success datum')])))

(defmethod local-request-handler :datum/get
  [app-state msg]
  (let [user-datum (build-datum-data @app-state (get msg :noun))]
    (hash-map :messages [(msg/get-datum-success user-datum)])))

(defmethod local-request-handler :literal/get
  [app-state msg]
  (let [user-literal (build-literal-data app-state (get msg :noun))]
    (hash-map :messages [(msg/get-literal-success user-literal)])))

; FIXME: what if update converts literal to datum?
(defmethod local-request-handler :literal/update
  [app-state msg]
  (let [literal-id (get-in msg [:noun :literal-id])
        {:keys [new-type new-value]} (get msg :noun)
        literal (get-in app-state [:cache literal-id])
        literal' (-> literal
                     (select-keys schema/literal-meta-keys)
                     (assoc new-type new-value))
        ]
    (hash-map :messages [(msg/update-literal-success literal')])))

