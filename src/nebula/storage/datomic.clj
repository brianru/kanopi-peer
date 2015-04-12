(ns nebula.storage.datomic
  "Datomic database component and datomic-specific helper functions."
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

(defrecord Database [host port conn]
  component/Lifecycle
  (start [this]
    (println "Starting database")
    (if conn this
      (assoc this :conn (connect-to-database host port))))

  (stop [this]
    (println "Stopping database")
    (if-not conn this
      (do
        (d/release conn)
        (assoc this :conn nil)))))

(defn database [host port]
  (map->Database {:host host :port port}))
