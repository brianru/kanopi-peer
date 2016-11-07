(ns kanopi.model.storage.datomic
  "Datomic database component and datomic-specific helper functions."
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io])
  (:import datomic.Datom))

;; FIXME: add tx datums including txInstant, set to last modified for
;; file
(defn- load-files! [conn files]
  (doseq [file-path files]
    (println "loading " file-path)
    (when-let [txdata (not-empty (read-string (slurp (io/resource file-path))))]
      @(d/transact conn txdata))))

;; FIXME: this should be in another ns.
(def auth-rules
  '[
    ;; Datums
    [(readable ?team ?e)
     [?e :datum/team ?team] ]

    ;; Facts
    [(readable ?team ?e)
     [?datum :datum/team ?team]
     [?datum :datum/fact ?e]]

    ;; Literals
    [(readable ?team ?e)
     [?e :literal/team ?team]]
    [(readable ?team ?e)
     [?literal :literal/team ?team]
     [?literal _ ?e]]

    ;; Transactions
    [(readable ?team ?e)
     [or
      [?ent :datum/team ?team]
      [?ent :literal/team ?team]]
     [?ent _ _ ?e]]

    ])

(defn authorized-entities
  ([base-db creds]
   (authorized-entities auth-rules base-db creds))
  ([rules base-db creds]
   (let [user-id (get-in creds [:ent-id])
         team-id (get-in creds [:current-team :db/id])
         ents   (d/q '[:find [?e ...]
                       :in $ % ?team
                       :where (readable ?team ?e)]
                     base-db rules team-id)]
     ;; NOTE: including team-id is important.
     (set (conj ents user-id team-id)))))

(defn filtered-db*
  ([base-db creds]
   (filtered-db* auth-rules base-db creds))
  ([rules base-db creds]
   (let [authorized-entities (authorized-entities rules base-db creds)]
     (d/filter base-db (fn [db ^Datom datom]
                         (contains? authorized-entities (.e datom)))))))

;; TODO: study https://www.youtube.com/watch?v=7lm3K8zVOdY
(defprotocol ISecureDatomic
  "Provide secured Datomic api fns based on provided credentials."
  (db [this creds] [this creds as-of]
    "API Consumers must think in terms of Peer processes instead of connection
    objects.
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

  ;; TODO: implement authorization controls
  ;; TODO: 2 ways to filter db, 1 for authenticated user, 1 for anonymous user
  ISecureDatomic
  (db [this creds]
    (if creds
      (filtered-db* (d/db connection) creds)
      (d/db connection)))

  (db [this creds as-of]
    (d/as-of (db this creds)))

  (transact [this creds txdata]
    (d/transact connection txdata))

  (entity [this creds ent-id]
    (d/entity (db this creds) ent-id)))

(defn datomic-peer [config]
  (map->DatomicPeer {:config config}))

