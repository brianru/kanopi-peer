(ns kanopi.controller.authorizer
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [datomic.api :as d]
            [kanopi.model.storage.datomic :as datomic]
   ))

(defprotocol IAuthorize
  (authorized-methods [this creds ent-id])
  (authorized? [this creds request])
  (enforce-entitlements [this creds]))

(defprotocol ICollaborate
  (create-team! [this username password teamname]
                "Anyone can create a new team, if the teamname is available.")
  (add-to-team! [this username password teamname new-user]
                "Users can be added to teams by existing members of that team.")
  (leave-team!  [this username password teamname]
                "Users can only be removed from a team by their own volition, they must choose to leave."))


;; TODO: implement this.
(defrecord AuthorizationService [config database]

  IAuthorize
  (authorized-methods [this creds ent-id]
    #{:get :put :post :delete})

  (authorized? [this creds request]
    (let [ent-id nil]
      (contains?
       (authorized-methods this creds ent-id)
       (:request-method request))))

  ;;(enforce-entitlements [this creds ])
  )

(defn new-authorization-service
  "Helper fn."
  ([]
   (new-authorization-service {}))
  ([config]
   (map->AuthorizationService {:config config})))
