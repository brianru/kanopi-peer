(ns kanopi.model.session
  "Session service."
  (:require [datomic.api :as d]
            [schema.core :as s]

            [kanopi.model.data :as data]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]

            [kanopi.controller.authenticator :as authenticator]
            ))

(defprotocol IKanopiSession
  (-hypothetical-db [this creds txdata]
                    "Used to initialize anonymous user sessions where the user
                    is not persisted.")
  (init-session [this creds])
  (init-anonymous-session [this])

  ;; TODO: session storage component.
  ;; in-memory riak? atom for dev move?
  (get-session! [this creds]
                "Retrieve session from session storage.")
  (put-session! [this creds session]
                "Persist session to session storage."))


(defrecord SessionService [config datomic-peer data-service authenticator]
  IKanopiSession

  (-hypothetical-db [this creds txdata]
    (d/with (datomic/db datomic-peer creds) txdata))

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
    (let [creds {}
          username nil
          password nil
          user-ent-id nil
          user-team-id nil
          txdata (concat
                    (authenticator/-init-team-data
                     authenticator (get-in creds [:current-team :db/id]))
                    (authenticator/-init-user-data
                     authenticator username password user-ent-id user-team-id)
                    )

          db (-hypothetical-db this creds txdata)]
      (hash-map
       :user creds
       :page {}
       :datum {}
       :most-viewed-datums []
       :most-edited-datums []
       :recent-datums      []
       :cache {})))

  (init-session [this creds]
    (let [
          ]
      (hash-map
       :user  creds
       ;; TODO: welcome to kanopi datum (b/c it's the users first time
       ;; if this is executing.))
       :page  {}
       :datum {} ;; TODO: if page is datum, use that, else {}
       :most-viewed-datums (data/most-viewed-datums data-service creds)
       :most-edited-datums (data/most-edited-datums data-service creds)
       :recent-datums      (data/recent-datums data-service creds)
       :cache {}
       ))))

(defn session-service [config]
  (map->SessionService {:config config}))
