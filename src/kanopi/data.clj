(ns kanopi.data
  "Generic API to a Database component.

  TODO: consistent naming convention for sync vs async requests. check
  datomic api for inspiration.
  "
  (:require [datomic.api :as d]))

(defn get-entity*
  "TODO: may want to use pull api to grab facts as well."
  [db ent-id]
  (->> ent-id
       (d/entity db)
       (into {})))


(defprotocol IDatabase
  (add-entity [this entity])
  (get-entity [this ent-id] [this as-of ent-id])
  (swap-entity [this entity'])
  (retract-entity [this ent-id])
  (assert-statements [this stmts]))

(defrecord DatomicDatabase [database]
  IDatabase
  (get-entity [database ent-id]
    (let [db (-> database :connection (d/db))]
      (get-entity* db ent-id)))
  (get-entity [database as-of ent-id]
    (let [db (-> database :connection (d/db as-of))]
      (get-entity* db ent-id)))
  )

(defn database []
  (map->DatomicDatabase {}))
