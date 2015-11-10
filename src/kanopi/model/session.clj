(ns kanopi.model.session
  "Session service."
  (:require [kanopi.model.data :as data]
            [kanopi.controller.authenticator :as authenticator]
            [schema.core :as s]
            [kanopi.model.schema :as schema]
            ))

(defprotocol IKanopiSession
  (init-session [this] [this creds]))

(defrecord SessionService [config data-service]
  IKanopiSession
  (init-session [this]
    (init-session this (authenticator/temp-user)))

  (init-session [this creds]
    (let []
      (hash-map
       :user  creds
       ;; TODO: if temp creds, direct to Welcome to Kanopi! datum.
       ;; otherwise, for now leave it empty. in the future this is
       ;; where we figure out the user's ideal homepage. 
       :page  {}
       :datum {}
       :most-viewed-datums [] ;(data/most-viewed-datums data-service creds)
       :most-edited-datums [] ;(data/most-edited-datums data-service creds)
       :recent-datums      [] ;(data/recent-datums data-service creds)
       :cache {}
       ))))

(defn session-service [config]
  (map->SessionService {:config config}))
