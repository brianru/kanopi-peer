(ns kanopi.model.session
  "Session service."
  (:require [datomic.api :as d]
            [schema.core :as s]

            [kanopi.model.data :as data]
            [kanopi.model.data.impl :as data-impl]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]

            [kanopi.controller.authenticator :as authenticator]

            [kanopi.util.core :as util]))


(defprotocol IKanopiSession
  (init-session [this creds])

  ;; TODO: session storage component.
  ; (get-session! [this creds]
  ;               "Retrieve session from session storage.")
  ; (put-session! [this creds session]
  ;               "Persist session to session storage.")
  )

(defrecord SessionService [config datomic-peer data-service authenticator]
  IKanopiSession
  (init-session [this creds]
    (let [welcome-ent-id
          (d/q '[:find ?e .
                 :in $
                 :where [?e :datum/label "Welcome to Kanopi!"]]
               (datomic/db datomic-peer creds))]
      (hash-map
       :user (dissoc creds :password)
       :page "/"
       ; :datum (data/user-datum* db welcome-ent-id)
       ; :most-viewed-datums (data/most-viewed-datums data-service creds)
       ; :most-edited-datums (data/most-edited-datums data-service creds)
       ; :recent-datums      (data/recent-datums data-service creds)
       :cache {}))))

(defn session-service [config]
  (map->SessionService {:config config}))
