(ns kanopi.model.session
  "Session service."
  (:require [datomic.api :as d]
            [schema.core :as s]

            [kanopi.model.data :as data]
            [kanopi.model.data.impl :as data-impl]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]

            [kanopi.controller.authenticator :as authenticator]

            [kanopi.util.core :as util]
            ))

(defprotocol IKanopiSession
  (-hypothetical-db [this creds txdata]
                    "Used to initialize anonymous user sessions where the user
                    is not persisted.")
  (init-session [this creds])
  (init-anonymous-session [this])

  ;; TODO: session storage component.
  ;; in-memory riak? atom for dev move?
  ; (get-session! [this creds]
  ;               "Retrieve session from session storage.")
  ; (put-session! [this creds session]
  ;               "Persist session to session storage.")
  )


(defrecord SessionService [config datomic-peer data-service authenticator]
  IKanopiSession

  (-hypothetical-db [this creds txdata]
    (let [report (d/with (datomic/db datomic-peer creds) txdata)]
      (get report :db-after)))

  ;; TODO: the obvious thing to do here is create a datomic db
  ;; with raw-data as hypothetical tx-data using datomic.api/with
  ;;
  ;; What's wrong with that?
  ;; A fair amount of work to do each time someone anonymously
  ;; requests the homepage. Do I care? The whole point is I want
  ;; to drive as many people as possible through this funnel. The
  ;; number of unauthenticated hits should be worth it. I can
  ;; cache the hell out of this. Put a CDN in front.
  (init-anonymous-session [this]
    (let [user-temp-id (d/tempid :db.part/users)
          team-temp-id (d/tempid :db.part/users)
          username     (util/random-uuid)
          password     nil
          txdata (->> (concat
                       (authenticator/-init-team-data authenticator team-temp-id)
                       (authenticator/-init-user-data
                        authenticator username password user-temp-id team-temp-id))
                      (remove nil?))

          db (-hypothetical-db this nil txdata)
          creds (authenticator/credentials* db username false)
          
          welcome-ent-id (d/q '[:find ?e .
                                :in $
                                :where [?e :datum/label "Welcome to Kanopi!"]] db)
          ]
      (hash-map
       :user  (dissoc creds :password)
       :page  (str "/datum/" welcome-ent-id)
       :datum (data/user-datum* db welcome-ent-id)
       :cache (hash-map welcome-ent-id
                        (data-impl/get-datum* db welcome-ent-id))
       )))

  (init-session [this creds]
    (let [db (datomic/db datomic-peer creds)
          welcome-ent-id (d/q '[:find ?e .
                                :in $
                                :where [?e :datum/label "Welcome to Kanopi!"]] db)
          ]
      (hash-map
       :user (dissoc creds :password)
       :page "/"
       ; :datum (data/user-datum* db welcome-ent-id)
       :most-viewed-datums (data/most-viewed-datums data-service creds)
       :most-edited-datums (data/most-edited-datums data-service creds)
       :recent-datums      (data/recent-datums data-service creds)
       :cache {}
       ))))

(defn session-service [config]
  (map->SessionService {:config config}))
