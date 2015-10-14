(ns kanopi.storage.datomic
  "Datomic database component and datomic-specific helper functions."
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defn- load-files! [conn files]
  (doseq [file-path files]
    (println "loading " file-path)
    (when-let [txdata (not-empty (read-string (slurp file-path)))]
      @(d/transact conn txdata))))

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

(defrecord DatomicPeer [config connection db-mode]
  component/Lifecycle
  (start [this]
    (println "starting datomic peer")
    (if connection
      this
      (let [uri (->> config ((juxt :uri :db-name)) (apply str))
            db-mode (-> (re-find #"datomic:([a-z]+):" uri) (last))  
            _ (when (or true (= db-mode "mem"))
                (d/delete-database uri)
                (d/create-database uri))
            conn (d/connect uri)
            ]
        (when (or true (= db-mode "mem"))
          (println "load schema")
          (load-files! conn (:schema config)))

        (when (:dev config)
          (println "load data")
          (load-files! conn (:data config)))
        
        (assoc this :connection conn :db-mode db-mode))))

  (stop [this]
    (println "stopping datomic peer")
    (if-not connection
      this
      (do
       (d/release connection)
       (assoc this :connection nil :db-mode nil))))

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

