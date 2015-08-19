(ns kanopi.storage.datomic
  "Datomic database component and datomic-specific helper functions."
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defn- load-files! [conn files]
  (doseq [file-path files]
    (println "loading " file-path)
    (when-let [txdata (not-empty (read-string (slurp file-path)))]
      @(d/transact conn txdata))))

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

;; TODO: study https://www.youtube.com/watch?v=7lm3K8zVOdY
(defprotocol ISecureDatomic
  "Provide secured Datomic api fns based on provided credentials."
  (db [this creds] [this creds as-of]
      "API Consumers must think in terms of Peer processes instead of connection objects.
      NOTE: remember user registration case (creds are nil)")
  (transact [this creds txdata]
            "Abstract from transact and transact-async. Pick one.
            NOTE: remember user registration case (creds are nil)"))

;; NOTE: in future, give DP zookeeper conn info so it can then find
;; the uri for the correct Datomic DB
(defrecord DatomicPeer [config host port connection]
  component/Lifecycle
  (start [this]
    (println "starting datomic peer")
    (if connection
      this
      (assoc this :connection
             (connect-to-database host port config))))

  (stop [this]
    (println "stopping datomic peer")
    (if-not connection
      this
      (do
        (d/release connection)
        (assoc this :connection nil))))
  
  ISecureDatomic
  (db [this creds]
    (d/db connection))

  (db [this creds as-of]
    (d/db connection as-of))

  (transact [this creds txdata]
    (d/transact connection txdata)))

(defn datomic-peer [host port config]
  (map->DatomicPeer {:host host, :port port, :config config}))

