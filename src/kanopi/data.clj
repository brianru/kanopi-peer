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

  (init-datum    [this creds])
  (update-datum-label [this creds datum-id label])
  (get-datum     [this creds datum-id]
                 [this creds as-of datum-id])

  (context-datums [this creds datum-id])
  (similar-datums [this creds datum-id])
  (user-datum [this creds datum-id])

  (most-edited-datums [this creds])
  (most-viewed-datums [this creds])
  (recent-datums [this creds])

  (add-fact      [this creds datum-id attribute value])
  (update-fact   [this creds fact-id attribute value]
                 "attribute and value can be nil.")

  (retract-datum [this creds ent-id]
                 "Assumes only 1 user has access, and thus retracting totally retracts it.
                 If more than 1 user has that access this must only retract the appropriate
                 role(s) from the entity.")
  )

(defrecord DatomicDataService [config datomic-peer]
  IDataService
  (init-datum [this creds]
    (let [datum  (mk-datum datomic-peer creds "banana boat" ["type" "welcome"])
          report @(datomic/transact datomic-peer creds (get datum :txdata))]
      (d/resolve-tempid (:db-after report) (:tempids report) (get datum :ent-id))))

  (update-datum-label [this creds datum-id label]
    (let [txdata [[:db/add datum-id :datum/label label]]
          report @(datomic/transact datomic-peer creds txdata)]
      (get-datum* (:db-after report) datum-id)))

  (get-datum [this creds ent-id]
    (let [db (datomic/db datomic-peer creds)]
      (get-datum* db ent-id)))

  (get-datum [this creds as-of ent-id]
    (let [db (datomic/db datomic-peer creds as-of)]
      (get-datum* db ent-id)))

  (context-datums [this creds ent-id]
    (let []
      (->> (d/q
            '[:find ?subj ?attr ?ent-id
              :in $ ?ent-id
              :where
              [?subj :datum/fact ?fact]
              [?fact :fact/attribute ?attr]
              [?fact :fact/value ?ent-id]]
            (datomic/db datomic-peer creds)
            ent-id)
           (sort-by first)
           )
      ))

  (similar-datums [this creds ent-id]
    ;; TODO: this should be more complex. sharing a fact is too
    ;; strict. what about similar attrs in different facts? (diff vals?)
    (let [datums-with-shared-facts []
          datums-with-shared-attrs []]
      (->> (d/q
            '[:find ?subj ?attr ?valu
              :in $ ?ent-id
              :where
              [?ent-id :datum/fact ?fact]
              [?subj :datum/fact ?fact]
              [(!= ?ent-id ?subj)]
              [?fact :fact/attribute ?attr]
              [?fact :fact/value ?valu]

              ]
            (datomic/db datomic-peer creds)
            ent-id)
           (sort-by first)
           )

      ))

  (user-datum [this creds datum-id]
    (hash-map :context-datums
              (context-datums this creds datum-id)
              :datum
              (get-datum this creds datum-id)
              :similar-datums
              (similar-datums this creds datum-id)
              ))

  (most-edited-datums [this creds]
    (let []
      ))

  (most-viewed-datums [this creds]
    (let []
      ))

  (recent-datums [this creds]
    (let [user-roles (->> creds :role (mapv :db/id))]
      (d/q '[:find ?e ?time ?tx
             :in $ [?user-role ...]
             :where
             [?e :datum/role ?user-role]
             [?e _ _ ?tx]
             [?tx :db/txInstant ?time]
             ]
           (datomic/db datomic-peer creds)
           user-roles)))

  (add-fact [this creds ent-id attribute value]
    (let [fact   (add-fact->txdata datomic-peer creds ent-id attribute value)
          txdata (conj (:txdata fact)
                       [:db/add ent-id :datum/fact (:ent-id fact)])
          report @(datomic/transact datomic-peer creds txdata)]
      (get-datum* (:db-after report) ent-id)))

  (update-fact [this creds fact-id attribute value]
    (let [fact-diff (update-fact->txdata datomic-peer creds fact-id attribute value)
          report    @(datomic/transact datomic-peer creds (:txdata fact-diff))]
      (get-fact* (:db-after report) fact-id)))

  (retract-datum [this creds ent-id]
    (let [{:keys [txdata]} (retract-entity->txdata datomic-peer creds ent-id)
          report @(datomic/transact datomic-peer creds txdata)]
      report)))

(defn data-service []
  (map->DatomicDataService {:config nil}))

