(ns nebula.storage.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defn- connect-to-database
  [host port]
  (let [uri (str "datomic:mem://nebula")]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)]
      @(d/transact conn (read-string (slurp "resources/schema.edn")))
      conn)))

(defrecord Database
    [host port conn]
  component/Lifecycle
  (start [component]
    (println "Starting database")
    (when-not conn
      (assoc component :conn (connect-to-database host port))))

  (stop [component]
    (println "Stopping database")
    (when conn
      (assoc component :conn nil))))

(defn database [host port]
  (map->Database {:host host :port port}))
