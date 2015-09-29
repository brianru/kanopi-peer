(ns kanopi.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]
            [clojure.pprint :refer (pprint)]
            [kanopi.util.core :as util]
            [kanopi.storage.datomic :as datomic]
            [kanopi.data.impl :refer :all]))

;; TODO: implement remove-fact
;;(remove-fact   [this creds ent-id fact-id])
(defprotocol IDataService
  (init-thunk    [this creds])
  (update-thunk  [this creeds thunk'])
  (get-thunk     [this creds thunk-id]
                 [this creds as-of thunk-id])
  (context-thunks [this creds thunk-id])
  (similar-thunks [this creds thunk-id])
  (user-thunk [this creds thunk-id]
              [this creds as-of thunk-id])
  (recent-thunks [this creds])
  (add-fact      [this creds thunk-id attribute value])
  (update-fact   [this creds fact-id attribute value]
                 "attribute and value can be nil.")
  (retract-thunk [this creds ent-id]
                 "Assumes only 1 user has access, and thus retracting totally retracts it.
                 If more than 1 user has that access this must only retract the appropriate
                 role(s) from the entity."))

(defrecord DatomicDataService [config datomic-peer]
  IDataService
  (init-thunk [this creds]
    (let [thunk  (mk-thunk datomic-peer creds "banana boat" ["type" "welcome"])
          report @(datomic/transact datomic-peer creds (get thunk :txdata))]
      (d/resolve-tempid (:db-after report) (:tempids report) (get thunk :ent-id))))

  (update-thunk [this creds thunk']
    )

  (get-thunk [this creds ent-id]
    (let [db (datomic/db datomic-peer creds)]
      (get-entity* db ent-id)))

  (get-thunk [this creds as-of ent-id]
    (let [db (datomic/db datomic-peer creds as-of)]
      (get-entity* db ent-id)))

  (context-thunks [this creds ent-id]
    (let []
      (->> (d/q
            '[:find ?s ?a ?ent-id
              :in $ ?ent-id
              :where [?s ?a ?ent-id]]
            (datomic/db datomic-peer creds)
            ent-id)
           (sort-by first)
           (partition-by first))
      ))

  (similar-thunks [this creds ent-id]
    (let []
      (->> (d/q
            '[:find ?s ?a ?v
              :in $ ?ent-id
              :where [?ent-id ?a ?v] [?s ?a ?v]]
            (datomic/db datomic-peer creds)
            ent-id)
           (sort-by first)
           (partition-by first)) 
      ))

  (user-thunk [this creds thunk-id]
    (hash-map :context-thunks
              (context-thunks this creds thunk-id)
              :thunk
              (get-thunk this creds thunk-id)
              :similar-thunks
              (similar-thunks this creds thunk-id)
              ))
  (user-thunk [this creds as-of thunk-id]
    (hash-map :context-thunks
              (context-thunks this creds thunk-id)
              :thunk
              (get-thunk this creds as-of thunk-id)
              :similar-thunks
              (similar-thunks this creds thunk-id)
              ))

  (recent-thunks [this creds]
    (let [user-id (-> creds :role)]
      (d/q '[:find [?e ?time]
             :in $ ?user-role
             :where
             [?e :thunk/role ?user-role]
             [?e _ _ ?tx]
             [?tx :db/txinstant ?time]
             ]
           (datomic/db datomic-peer creds)
           user-id)))

  (add-fact [this creds ent-id attribute value]
    (let [fact   (add-fact->txdata datomic-peer creds ent-id attribute value)
          txdata (conj (:txdata fact)
                       [:db/add ent-id :thunk/fact (:ent-id fact)])
          report @(datomic/transact datomic-peer creds txdata)]
      (get-entity* (:db-after report) ent-id)))

  (update-fact [this creds fact-id attribute value]
    (let [fact-diff (update-fact->txdata datomic-peer creds fact-id attribute value)
          report    @(datomic/transact datomic-peer creds (:txdata fact-diff))]
      (get-entity* (:db-after report) fact-id)))

  (retract-thunk [this creds ent-id]
    (let [{:keys [txdata]} (retract-entity->txdata datomic-peer creds ent-id)
          report @(datomic/transact datomic-peer creds txdata)]
      report)))

(defn data-service []
  (map->DatomicDataService {:config nil}))

