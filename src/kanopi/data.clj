(ns kanopi.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]
            [kanopi.storage.datomic :as datomic]))

(defprotocol IDataService
  (init-thunk     [this creds])
  (get-thunk      [this creds thunk-id]
                  [this creds as-of thunk-id])
  (entity?        [this creds ent-id])
  (add-fact       [this creds thunk-id attribute value])
  (swap-entity    [this creds entity'])
  (retract-entity [this creds ent-id])
  )

(defn- describe-input [datomic-peer creds input]
  (cond
   (entity? datomic-peer creds input)
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
   :txdata nil))

(defn get-entity*
  "TODO: may want to use pull api to grab facts as well."
  [db ent-id]
  (->> ent-id
       (d/entity db)
       (into {})))


(defn- mk-thunk
  "TODO: use default label"
  ([datomic-peer creds]
   (mk-thunk datomic-peer creds ""))
  ([datomic-peer creds label]
   (hash-map
    :ent-id nil
    :txdata nil)))

(defn- mk-attribute
  "TODO: use this."
  [datomic-peer creds attribute]
  (hash-map
   :attribute-id nil
   :txdata nil))

(defn- mk-value 
  "TODO: use this."
  [datomic-peer creds value]
  (hash-map
   :value-id nil
   :txdata nil))

(defmethod fact->txdata [::entity-id ::entity-id]
  [datomic-peer creds ent-id attribute value]
  (let [fact-id #db/id [:db.part/structure]]
    (hash-map
     :fact-id fact-id
     :txdata  [[fact-id :fact/attribute attribute]
               [fact-id :fact/value     value]
               ]
     )))

(defmethod fact->txdata [::string ::entity-id]
  [datomic-peer creds ent-id attribute value]
  (let [fact-id      #db/id [:db.part/structure]
        attribute-id #db/id [:db.part/structure]]
    (hash-map
     :fact-id fact-id
     :txdata [[fact-id      :fact/attribute attribute-id]
              [fact-id      :fact/value     value]
              [attribute-id :thunk/role     (:username creds)]
              [attribute-id :thunk/label    attribute]
              ])))

(defmethod fact->txdata [::entity-id ::string]
  [datomic-peer creds ent-id attribute value]
  (let [fact-id  #db/id [:db.part/structure]
        value-id #db/id [:db.part/structure]]
    (hash-map
     :fact-id fact-id
     :txdata [[fact-id  :fact/attribute attribute]
              [fact-id  :fact/value     value-id]
              [value-id :value/string   value]
              ])))

(defmethod fact->txdata [::string ::string]
  [datomic-peer creds ent-id attribute value]
  (let [fact-id #db/id [:db.part/structure]
        attribute-id #db/id [:db.part/structure]
        value-id #db/id [:db.part/structure]]
    (hash-map
     :fact-id fact-id
     :txdata [[fact-id :fact/attribute attribute-id]
              [fact-id :fact/value     value-id]
              [attribute-id :thunk/role (:username creds)]
              [attribute-id :thunk/label attribute]
              [value-id :value/string  value]
              ])))

(defrecord DatomicDataService [datomic-peer]
  IDataService
  (init-thunk [this creds]
    (let [txdata [{:db/id #db/id [:db.part/structure]
                   :thunk/role (:username creds) ;; role from creds
                   :thunk/label nil ;; default-label
                   }
                  ]
          res (datomic/transact datomic-peer creds txdata)]
      res))

  (get-thunk [this creds ent-id]
    (let [db (datomic/db datomic-peer creds)]
      (get-entity* db ent-id)))

  (get-thunk [this creds as-of ent-id]
    (let [db (datomic/db datomic-peer as-of)]
      (get-entity* db ent-id)))

  (add-fact [this creds ent-id attribute value]
    (let [fact-txdata (fact->txdata datomic-peer creds ent-id attribute value)
          txdata (conj (:txdata fact-txdata)
                       [ent-id :thunk/fact (:fact-id fact-txdata)])
          res (datomic/transact datomic-peer creds txdata)]))

  (swap-entity [this creds ent']
    nil)

  (retract-entity [this creds ent-id]
    nil))

(defn data-service []
  (map->DatomicDataService {}))
