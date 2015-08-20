(ns kanopi.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]
            [clojure.pprint :refer (pprint)]
            [kanopi.storage.datomic :as datomic]))

(defprotocol IDataService
  (init-thunk    [this creds])
  (get-thunk     [this creds thunk-id]
                 [this creds as-of thunk-id])
  (user-thunk    [this creds] [this creds as-of])
  (add-fact      [this creds thunk-id attribute value])
  (swap-entity   [this creds entity'])
  (retract-thunk [this creds ent-id]
                 "Assumes only 1 user has access, and thus retracting totally retracts it.
                 If more than 1 user has that access this must only retract the appropriate
                 role(s) from the entity.")
  )

(defn- describe-input [datomic-peer creds input]
  (cond
   (and integer? (datomic/entity datomic-peer creds input))
   ::entity-id

   (string? input)
   ::string

   ))

(defmulti fact->txdata
  "Generate transaction data for asserting the given [eid attr value].

  Consumers will pass either literals of entity refs to attr and value. The methods should take those inputs and generate the necessary transaction data (including nested entities, as necessary).
  
  NOTE: at this stage we're only working with references and strings."
 (fn [datomic-peer creds ent-id attribute value]
   (mapv (partial describe-input datomic-peer creds)
         [attribute value])))

(defn retract-entity->txdata
  "Generate transaction data for retracting the specified entity."
  [datomic-peer creds ent-id]
  (hash-map
   :ent-id nil
   :txdata [[:db.fn/retractEntity ent-id]]))

(defn get-entity*
  "TODO: may want to use pull api to grab facts as well."
  [db ent-id]
  (when-let [ent (not-empty (d/touch (d/entity db ent-id)))]
    (->> ent
         (mapcat (fn [[k v]]
                   (let [v' (cond
                             (instance? datomic.query.EntityMap v)
                             (list v)

                             (every? (partial instance? datomic.query.EntityMap) v)
                             v

                             (not (set? v))
                             (list v)
                             )
                         ;; ensure it's a set
                         v'' (if (set? v') v' (set v'))]
                     [k v''])))
         (apply hash-map))))

(defn- mk-attribute
  "FIXME: if attribute entity with same label? already exists, use it.
  TODO: Attributes are thunks. Should this call `mk-thunk`?"
  [datomic-peer creds attribute]
  (let [attribute-id (d/tempid :db.part/structure)]
    (hash-map
     :ent-id attribute-id
     :txdata [[:db/add attribute-id :thunk/role (get creds :role)]
              [:db/add attribute-id :thunk/label attribute]])))

(defn- describe-value-literal [value]
  (cond
   (string? value)
   :value/string))

(defn- mk-value 
  "FIXME: if value entity with same literal already exists, use it."
  [datomic-peer creds value]
  (let [value-id (d/tempid :db.part/structure)
        value-attribute (describe-value-literal value)]
    (hash-map
     :ent-id value-id
     :txdata [[:db/add value-id value-attribute value]])))

(defn- mk-fact
  [datomic-peer creds attribute value]
  (let [fact-id (d/tempid :db.part/structure)]
    (hash-map
     :ent-id fact-id
     :txdata  [[:db/add fact-id :fact/attribute attribute]
               [:db/add fact-id :fact/value     value] ])))

(defn- mk-thunk
  "ex: (mk-thunk dp creds label [attr-id val-id] [attr-literal val-id] ... etc)"
  ([datomic-peer creds]
   (mk-thunk datomic-peer creds ""))

  ([datomic-peer creds label]
   (let [thunk-id (d/tempid :db.part/structure)]
     (hash-map
      :ent-id thunk-id
      :txdata [[:db/add thunk-id :thunk/role (get creds :role)]
               [:db/add thunk-id :thunk/label label]])))

  ([datomic-peer creds label & facts]
   (let [thunk-id  (d/tempid :db.part/structure)
         facts     (map (partial apply fact->txdata datomic-peer creds thunk-id) facts)
         fact-coll (reduce
                    (fn [{:keys [ent-ids txdata] :as acc} fact]
                      (hash-map :ent-ids (conj ent-ids (:ent-id fact))
                                :txdata (concat txdata (:txdata fact))))
                    {:ent-ids []
                     :txdata  []}
                    facts)]
     (hash-map
      :ent-id thunk-id
      :txdata (concat [[:db/add thunk-id :thunk/role  (get creds :role)]
                       [:db/add thunk-id :thunk/label label]]
                      (mapv (partial vector :db/add thunk-id :thunk/fact)
                            (get fact-coll :ent-ids))
                      (get fact-coll :txdata))))))

(defmethod fact->txdata [::entity-id ::entity-id]
  [datomic-peer creds ent-id attribute value]
  (let [fact (mk-fact datomic-peer creds attribute value) ]
    (hash-map
     :ent-id  (get fact :ent-id)
     :txdata  (conj (get fact :txdata)
                    [:db/add ent-id :thunk/fact (get fact :ent-id)]))))

(defmethod fact->txdata [::string ::entity-id]
  [datomic-peer creds ent-id attribute value]
  (let [attr (mk-attribute datomic-peer creds attribute)
        fact (mk-fact datomic-peer creds (get :ent-id attr) value)]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get attr :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :thunk/fact (get fact :ent-id)]]))))

(defmethod fact->txdata [::entity-id ::string]
  [datomic-peer creds ent-id attribute value]
  (let [value (mk-value datomic-peer creds value)
        fact  (mk-fact datomic-peer creds attribute (get value :ent-id)) ]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get value :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :thunk/fact (get fact :ent-id)]]))))

(defmethod fact->txdata [::string ::string]
  [datomic-peer creds ent-id attribute value]
  (let [attr (mk-attribute datomic-peer creds attribute)
        value (mk-value datomic-peer creds value)
        fact (mk-fact datomic-peer creds (get attr :ent-id) (get value :ent-id))]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get value :txdata)
                     (get attr :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :thunk/fact (get fact :ent-id)]]))))

(defrecord DatomicDataService [config datomic-peer]
  IDataService
  (init-thunk [this creds]
    (let [thunk  (mk-thunk datomic-peer creds "banana boat" ["type" "welcome"])
          report @(datomic/transact datomic-peer creds (get thunk :txdata))]
      (d/resolve-tempid (:db-after report) (:tempids report) (get thunk :ent-id))))

  (get-thunk [this creds ent-id]
    (let [db (datomic/db datomic-peer creds)]
      (get-entity* db ent-id)))

  (get-thunk [this creds as-of ent-id]
    (let [db (datomic/db datomic-peer creds as-of)]
      (get-entity* db ent-id)))

  (add-fact [this creds ent-id attribute value]
    (let [fact-txdata (fact->txdata datomic-peer creds ent-id attribute value)
          txdata (conj (:txdata fact-txdata)
                       [ent-id :thunk/fact (:fact-id fact-txdata)])
          report @(datomic/transact datomic-peer creds txdata)]
      ;; FIXME: return something more directly useful
      report))

  (swap-entity [this creds ent']
    nil)

  (retract-thunk [this creds ent-id]
    (let [{:keys [txdata]} (retract-entity->txdata datomic-peer creds ent-id)
          report @(datomic/transact datomic-peer creds txdata)]
      report)))

(defn data-service []
  (map->DatomicDataService {:config nil}))

