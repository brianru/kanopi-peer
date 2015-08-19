(ns kanopi.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]
            [kanopi.storage.datomic :as datomic]))

(defn get-entity*
  "TODO: may want to use pull api to grab facts as well."
  [db ent-id]
  (->> ent-id
       (d/entity db)
       (into {})))

(defprotocol IDatabase
  (init-thunk [this creds])
  (get-thunk [this creds thunk-id] [this creds as-of thunk-id])
  (add-fact [this creds thunk-id attribute value])
  (swap-entity [this creds entity'])
  (retract-entity [this creds ent-id])
  )

(defrecord DatomicDatabase [database]
  IDatabase
  (init-thunk [this creds]
    (let [db (datomic/db database creds)]
      [nil nil]))

  (get-thunk [this creds ent-id]
    (let [db (-> database :connection (d/db))]
      (get-entity* db ent-id)))
  (get-thunk [this creds as-of ent-id]
    (let [db (-> database :connection (d/db as-of))]
      (get-entity* db ent-id)))
  (add-fact [this creds ent-id attribute value]
    nil)
  (swap-entity [this creds ent']
    nil)
  (retract-entity [this creds ent-id]
    nil)
  )

(defn database []
  (map->DatomicDatabase {}))
