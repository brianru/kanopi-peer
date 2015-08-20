(ns kanopi.storage.datomic
  "Datomic database component and datomic-specific helper functions."
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defn- load-files! [conn files]
  (doseq [file-path files]
    (println "loading " file-path)
    (when-let [txdata (not-empty (read-string (slurp file-path)))]
      @(d/transact conn txdata))))

(defn- connect-to-database [config]
  (let [uri (:uri config)]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

;; TODO: study https://www.youtube.com/watch?v=7lm3K8zVOdY
(defprotocol ISecureDatomic
  "Provide secured Datomic api fns based on provided credentials."
  (db [this creds] [this creds as-of]
      "API Consumers must think in terms of Peer processes instead of connection objects.
      NOTE: remember user registration case (creds are nil)")
  (transact [this creds txdata]
            "Abstract from transact and transact-async. Pick one.
            NOTE: remember user registration case (creds are nil)")
  (entity [this creds ent-id]))

;; NOTE: in future, give DP zookeeper conn info so it can then find
;; the uri for the correct Datomic DB
(defrecord DatomicPeer [config connection]
  component/Lifecycle
  (start [this]
    (println "starting datomic peer")
    (if connection
      this
      (let [conn (connect-to-database config)]

        (println "load schema")
        (load-files! conn (:schema config))

        (when (:dev config)
          (println "load data")
          (load-files! conn (:data config)))
        
        (assoc this :connection conn))))

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
    (d/transact connection txdata))
  
  (entity [this creds ent-id]
    (d/entity (db this creds) ent-id)))

(defn datomic-peer [config]
  (map->DatomicPeer {:config config}))

