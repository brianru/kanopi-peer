(ns nebula.storage.datomic
  "Datomic database component and datomic-specific helper functions."
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defn- connect-to-database
  [host port config]
  (let [uri (str "datomic:mem://nebula")]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (for [file-path (:schema config)]
        @(d/transact conn (read-string (slurp file-path))))
      conn)))

(defrecord Database [config host port conn]
  component/Lifecycle
  (start [this]
    (println "starting database")
    (if conn this
        (assoc this :conn (connect-to-database host port config))))

  (stop [this]
    (println "stopping database")
    (if-not conn this
            (do
              (d/release conn)
              (assoc this :conn nil)))))

(defn database [host port config]
  (map->Database {:host host, :port port, :config config}))
