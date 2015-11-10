(ns kanopi.model.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]
            [clojure.pprint :refer (pprint)]
            [kanopi.util.core :as util]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.model.data.impl :refer :all]))

;; TODO: implement remove-fact
;;(remove-fact   [this creds ent-id fact-id])
(defprotocol IDataService

  (init-datum    [this creds])
  (update-datum-label [this creds datum-id label])
  (get-datum     [this creds datum-id]
             [this creds as-of datum-id])
  (-get-fact [this creds fact-id])
  (search-datums [this creds search])

  (context-datums [this creds datum-id])
  (similar-datums [this creds datum-id])
  (user-datum [this creds datum-id])

  (most-edited-datums [this creds])
  (most-viewed-datums [this creds])
  (recent-datums [this creds])

  ;; TODO: add arities accepting fact as an entity map? this is how
  ;; the client sends the facts. the extra arity would just parse the
  ;; entity map into the attr/value input shape [_tag_ _value_]]
  (add-fact [this creds datum-id]
            [this creds datum-id fact]
            [this creds datum-id attribute value])
  (update-fact [this creds fact]
               [this creds fact-id attribute value]
               "attribute and value can be nil.")

  (retract-datum [this creds ent-id]
                 "Assumes only 1 user has access, and thus retracting totally retracts it.
                 If more than 1 user has that access this must only retract the appropriate
                 team(s) from the entity."))

(defn annotate-transaction
  "TODO: add tx datums
  - who did it
  - action performed"
  [txdata action creds]
  txdata)

(defrecord DatomicDataService [config datomic-peer]
  IDataService
  (init-datum [this creds]
    (let [datum  (mk-datum datomic-peer creds)
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

  (-get-fact [this creds ent-id]
    (let [db (datomic/db datomic-peer creds)]
      (get-fact* db ent-id)))

  (search-datums [this creds search]
    (let [db (datomic/db datomic-peer creds)
          results (d/q '[:find ?score ?entity
                         :in $ ?search
                         :where [(fulltext $ :datum/label ?search) [[?entity ?name ?tx ?score]]]
                         ]
                       db search)]
      (or results [])))

  (context-datums [this creds ent-id]
    (let [matches (->> (d/q
                        '[:find ?subj ?attr
                          :in $ ?ent-id
                          :where
                          [?subj :datum/fact ?fact]
                          [?fact :fact/attribute ?attr]
                          [?fact :fact/value ?ent-id]]
                        (datomic/db datomic-peer creds)
                        ent-id)
                       (take 10))
          ]
      (mapv (fn [result]
              (mapv (partial get-datum this creds) result))
            matches)))

  (similar-datums [this creds ent-id]
    ;; TODO: this should be more complex. sharing a fact is too
    ;; strict. what about similar attrs in different facts? (diff vals?)
    (let [datums-with-shared-facts []
          datums-with-shared-attrs []
          matches (->> (d/q
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
                       (take 10))
          ]
      (mapv (fn [result]
              (mapv (partial get-datum this creds) result))
            matches)))

  (user-datum [this creds datum-id]
    (hash-map :context-datums
              (context-datums this creds datum-id)
              :datum
              (get-datum this creds datum-id)
              :similar-datums
              (similar-datums this creds datum-id)
              ))

  (most-edited-datums [this creds]
    (let [user-teams (->> creds :teams (mapv :db/id))
          results (->> (d/q '[:find ?e (count-distinct ?tx)
                              :in $ [?user-team ...]
                              :where
                              [?e :datum/team ?user-team]
                              [?e _ _ ?tx]]
                            (datomic/db datomic-peer creds)
                            user-teams)
                       (take 10))]
      (mapv (fn [[datum-id cnt]]
              (vector (get-datum this creds datum-id) cnt))
            results)))

  (most-viewed-datums [this creds]
    (let []
      []))

  ;; TODO: filter by recency
(recent-datums [this creds]
               (let [user-teams (->> creds :teams (mapv :db/id))
                     results    (->> (d/q '[:find ?e ?time ?tx
                                            :in $ [?user-team ...]
                                            :where
                                            [?e :datum/team ?user-team]
                                            [?e _ _ ?tx]
                                            [?tx :db/txInstant ?time]
                                            ]
                                          (datomic/db datomic-peer creds)
                                          user-teams)
                                     )]
                 (mapv (fn [[datum-id tm tx]]
                         (vector (get-datum this creds datum-id) tm tx))
                       results)))

(add-fact [this creds ent-id {:keys [fact/attribute fact/value] :as fact}]
          (add-fact this creds ent-id (entity->input attribute) (entity->input value)))

(add-fact [this creds ent-id attribute value]
          (assert (every? identity [attribute value])
                  "Must submit fact with both attribute and value.")
          (let [fact   (add-fact->txdata datomic-peer creds ent-id attribute value)
                txdata (conj (:txdata fact)
                             [:db/add ent-id :datum/fact (:ent-id fact)])
                report @(datomic/transact datomic-peer creds txdata)]
            (when (not-empty (get report :tx-data))
              (get-datum this creds ent-id))))

(update-fact [this creds {:keys [db/id fact/attribute fact/value] :as fact}]
             (update-fact this creds id (entity->input attribute) (entity->input value)))

(update-fact [this creds fact-id attribute value]
             (let [fact-diff (update-fact->txdata datomic-peer creds fact-id attribute value)
                   report    @(datomic/transact datomic-peer creds (:txdata fact-diff))]
               (when (not-empty (get report :tx-data))
                 (-get-fact this creds fact-id))))

(retract-datum [this creds ent-id]
               (let [{:keys [txdata]} (retract-entity->txdata datomic-peer creds ent-id)
                     report @(datomic/transact datomic-peer creds txdata)]
                 report)))

(defn data-service []
  (map->DatomicDataService {:config nil}))

