(ns kanopi.model.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]
            [schema.core :as s]
            [clojure.pprint :refer (pprint)]
            [kanopi.util.core :as util]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.model.schema :as schema]
            [kanopi.model.data.impl :refer :all]))

(defn context-datums* [db datum-id]
  (let [matches (->> (d/q
                      '[:find ?subj ?attr
                        :in $ ?ent-id
                        :where
                        [?subj :datum/fact ?fact]
                        [?fact :fact/attribute ?attr]
                        [?fact :fact/value ?ent-id]]
                      db datum-id)
                     (take 10))
        ]
    (mapv (fn [result]
            (mapv (partial get-datum* db) result))
          matches)))

(defn similar-datums* [db datum-id]
  ;; TODO: this should be more complex. sharing a fact is too
  ;; strict. what about similar attrs in different facts? (diff vals?)
  (let [datums-with-shared-facts (->> (d/q
                                       '[:find ?subj ?attr ?valu
                                         :in $ ?ent-id
                                         :where
                                         [?ent-id :datum/fact ?fact]
                                         [?subj :datum/fact ?fact]
                                         [(!= ?ent-id ?subj)]
                                         [?fact :fact/attribute ?attr]
                                         [?fact :fact/value ?valu]

                                         ]
                                       db datum-id)
                                      (take 10))
        datums-with-shared-attrs (->> (d/q
                                       '[:find ?subj ?attr ?valu
                                         :in $ ?ent-id
                                         :where
                                         [?ent-id :datum/fact ?fact]
                                         [?fact :fact/attribute ?attr]
                                         [(!= ?ent-id ?subj)]
                                         [?subj :datum/fact ?subj-fact]
                                         [?subj-fact :fact/attribute ?attr]
                                         [?subj-fact :fact/value ?valu]
                                         ]
                                       db datum-id)
                                      (take 10))
        datums-referenced-with-same-attr []
        ;; only matches same facts
        matches  (concat datums-with-shared-facts datums-with-shared-attrs)
        ]
    (mapv (fn [result]
            (mapv (partial get-datum* db) result))
          matches)))

(defn user-datum* [db datum-id]
  (hash-map :context-datums (context-datums* db datum-id)
            :datum          (get-datum* db datum-id)
            :similar-datums (similar-datums* db datum-id)))

(defn user-literal* [db literal-id]
  (hash-map :literal        (get-literal* db literal-id)
            :context-datums (context-datums* db literal-id)))

;; TODO
(defn user-transactions [db user-entity-id]
  (d/q '[:find [?tx ?txInstant]
         :in $ ?user-entity-id
         :where
         [?tx :db.tx/byUser ?user-entity-id]
         [?tx :db/txInstant ?txInstant]
         ]
       db user-entity-id))

(defn user-team-transactions [db user-entity-id])

(defprotocol IDatumService
  (init-datum [this creds])
  (update-datum-label [this creds datum-id label])
  (get-datum [this creds datum-id]
    [this creds as-of datum-id])
  (-get-fact [this creds fact-id]
    "Not sure whether this should be here. Do any DataService
             consumers call it or should this facility only be
             available through the impl namespace as a helper fn?")
  (search-datums [this creds search])
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
                 team(s) from the entity.")
  ;; TODO: implement remove-fact
  ;;(remove-fact   [this creds ent-id fact-id])
  )

(defprotocol ILiteralService
  (-init-literal [this creds]
    "I don't know if we actually want this. Currently,
                 all literals are created when creating fact parts
                 with a literal type.")
  (get-literal [this creds literal-id])
  (update-literal [this creds literal-id input]
    [this creds literal-id tp value]))

(defprotocol IUserDataService
  (context-datums [this creds datum-id])
  (similar-datums [this creds datum-id])
  (user-datum [this creds datum-id])
  (most-edited-datums [this creds])
  (most-viewed-datums [this creds])
  (recent-activity [this creds])
  (user-literal [this creds literal-id])
  )

(defn annotate-txdata
  "TODO: add tx datums
  - who did it
  - action performed
  - who what where when why how"
  [txdata user-id]
  (let [tx-id (d/tempid :db.part/tx)]
    (concat txdata
            [[:db/add tx-id :tx/byUser user-id]])))

(defrecord DatomicDataService [config datomic-peer]
  IDatumService
  (init-datum [this creds]
    (let [datum  (mk-datum datomic-peer creds)
          txdata (annotate-txdata (get datum :txdata) (:ent-id creds))
          report @(datomic/transact datomic-peer creds txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) (get datum :ent-id))))

  (update-datum-label [this creds datum-id label]
    (let [txdata (annotate-txdata [[:db/add datum-id :datum/label label]]
                                  (:ent-id creds))
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
    (when-not (s/check schema/QueryString search)
      (let [search (str search "*")
            db     (datomic/db datomic-peer creds)
            datum-results
            (d/q '[:find ?score ?entity
                   :in $ ?search
                   :where [(fulltext $ :datum/label ?search)
                           [[?entity ?name ?tx ?score]]]
                   ]
                 db search)

            literal-results
            (d/q '[:find ?score ?entity
                   :in $ ?search
                   :where [(fulltext $ :literal/text ?search)
                           [[?entity ?name ?tx ?score]]]]
                 db search)

            results (->> (concat datum-results literal-results)
                         (map (fn [[score ent-id]]
                                (vector score (get-datum* db ent-id))))
                         (sort-by first))]
        (or results #{}))))

  (add-fact [this creds ent-id {:keys [fact/attribute fact/value] :as fact}]
    (add-fact this creds ent-id (entity->input attribute) (entity->input value)))

  (add-fact [this creds ent-id attribute value]
    (assert (every? identity [attribute value])
            "Must submit fact with both attribute and value.")
    (let [fact   (add-fact->txdata datomic-peer creds ent-id attribute value)
          txdata (-> (:txdata fact)
                     (conj [:db/add ent-id :datum/fact (:ent-id fact)])
                     (annotate-txdata creds))
          report @(datomic/transact datomic-peer creds txdata)]
      (when (not-empty (get report :tx-data))
        (get-datum this creds ent-id))))

  (update-fact [this creds {:keys [db/id fact/attribute fact/value] :as fact}]
    (update-fact this creds id (entity->input attribute) (entity->input value)))

  (update-fact [this creds fact-id attribute value]
    (let [fact-diff (update-fact->txdata datomic-peer creds fact-id attribute value)
          txdata    (-> (:txdata fact-diff)
                        (annotate-txdata (:ent-id creds)))
          report    @(datomic/transact datomic-peer creds txdata)]
      (when (not-empty (get report :tx-data))
        (-get-fact this creds fact-id))))

  (retract-datum [this creds ent-id]
    (let [retraction (retract-entity->txdata datomic-peer creds ent-id)
          txdata     (annotate-txdata (:txdata retraction) (:ent-id creds))
          report     @(datomic/transact datomic-peer creds txdata)]
      report))

  ILiteralService
  (-init-literal [this creds]
    (let [literal (mk-literal datomic-peer creds "")
          txdata  (-> literal :txdata (annotate-txdata (:ent-id creds)))
          report  @(datomic/transact datomic-peer creds txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) (get literal :ent-id))))

  (get-literal [this creds literal-id]
    (let [db (datomic/db datomic-peer creds)]
      (get-literal* db literal-id)))

  (update-literal [this creds literal-id input]
    (apply update-literal this creds literal-id (describe-value-literal input)))
  (update-literal [this creds literal-id tp value]
    (assert (get schema/input-types tp)
            "Must provide a known input type.")
    (let [{:keys [txdata]}
          (update-literal->txdata datomic-peer creds literal-id tp value)
          txdata (annotate-txdata txdata (:ent-id creds))
          report @(datomic/transact datomic-peer creds txdata)]
      (get-literal* (:db-after report) literal-id)))

  IUserDataService
  (context-datums [this creds ent-id]
    (context-datums* (datomic/db datomic-peer creds) ent-id))

  (similar-datums [this creds ent-id]
    (similar-datums* (datomic/db datomic-peer creds) ent-id))

  (user-datum [this creds datum-id]
    (user-datum* (datomic/db datomic-peer creds) datum-id))

  (most-edited-datums [this creds]
    (let [user-teams (->> creds :teams (mapv :db/id))
          results    (->> (d/q '[:find ?e (count-distinct ?tx)
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
  (recent-activity [this creds]
    (let [txs (datomic/tx-data datomic-peer creds)]
      txs))

  (user-literal [this creds literal-id]
    (user-literal* (datomic/db datomic-peer creds) literal-id)))

(defn data-service []
  (map->DatomicDataService {:config nil}))
