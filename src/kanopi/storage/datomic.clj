(ns kanopi.storage.datomic
  "Datomic database component and datomic-specific helper functions."
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defn- load-files! [conn files]
  (doseq [file-path files]
    (println "loading " file-path)
    @(d/transact conn (read-string (slurp file-path)))))

(defn- connect-to-database
  [host port config]
  (let [uri (str "datomic:mem://kanopi")]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (println "load schema")
      (load-files! conn (:schema config))

      (println "load data")
      (load-files! conn (:data config))

      conn)))

(defrecord DatomicPeer [config host port connection]
  component/Lifecycle
  (start [this]
    (println "starting database")
    (if connection
      this
      (assoc this :connection
             (connect-to-database host port config))))

  (stop [this]
    (println "stopping database")
    (if-not connection
      this
      (do
        (d/release connection)
        (assoc this :connection nil)))))

(defn datomic-peer [host port config]
  (map->DatomicPeer {:host host, :port port, :config config}))
