(ns nebula.data
  "Generic API to a Database component.

  TODO: query protocol
  "
  (:require [datomic.api :as d]))

(defprotocol IQuery
  "docstring"
  (get-entity [this ent-id])
  )

(defrecord QueryService [database]
    IQuery
    (get-entity [this user ent-id]
      (let [filtered-db nil]
        (->> ent-id
             (d/entity filtered-db)
             (into {}))))
  )
