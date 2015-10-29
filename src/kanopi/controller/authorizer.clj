(ns kanopi.controller.authorizer
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [datomic.api :as d]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]
   ))

;; FIXME: rethink this protocol
(defprotocol IAuthorize
  (enforce-entitlements [this creds])
  (authorized-methods   [this creds ent-id])
  (authorized?          [this creds request])
  )

(defprotocol ICollaborate
  (create-team! [this creds teamname]
                "Anyone can create a new team, if the teamname is available.")
  (add-to-team! [this creds teamname new-user]
                "Users can be added to teams by existing members of that team.")
  ;; TODO: should leave-team! delete the team if creds represents the
  ;; last user in the team? Probably not, it should probably confirm
  ;; with the user then trigger a delete-team msg, which means we need
  ;; another function here.
  (leave-team!  [this creds teamname]
                "Users can only be removed from a team by their own volition, they must choose to leave."))


(defrecord AuthorizationService [config database]
  ICollaborate
  (create-team! [this {:keys [username password] :as creds} teamname]
    (assert (s/validate schema/InputCredentials [username password]))
    (assert (nil? (d/entid (datomic/db database nil) [:team/id teamname])))
    (let [team-id (d/tempid :db.part/users -1000)
          user-id (d/entid (datomic/db database creds) [:user/id username])

          txdata  [[:db/add team-id :team/id teamname]
                   [:db/add user-id :user/team team-id]
                   ]
          report  @(datomic/transact database nil txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) team-id)))

  (add-to-team! [this {:keys [username password] :as creds} teamname new-user]
    (assert (s/validate schema/InputCredentials [username password]))
    (assert (s/validate schema/UserId new-user))
    (assert (not= username teamname)
            "Cannot add other users to your personal team (private).")
    (assert (not= username new-user)
            "Cannot ever add yourself to a team.")
    (let [db      (datomic/db database nil)
          user-id (d/entid db [:user/id new-user])
          team-id (d/entid db [:team/id teamname])
          txdata  [[:db/add user-id :user/team team-id]]
          report  @(datomic/transact database creds txdata)]
      report))

  (leave-team! [this {:keys [username password] :as creds} teamname]
    (assert (s/validate schema/InputCredentials [username password]))
    (assert (not= teamname username)
            "Cannot leave personal team.")
    (assert (contains? (->> (get creds :teams)
                            (map :team/id)
                            (set))
                       teamname)
            "Can only leave current teams.")
    (let [db      (datomic/db database creds)
          user-id (d/entid db [:user/id username])
          team-id (d/entid db [:team/id teamname])
          txdata  [[:db/retract user-id :user/team team-id]]
          report  @(datomic/transact database creds txdata)]
      report))
  
  )

(defn new-authorization-service
  "Helper fn."
  ([]
   (new-authorization-service {}))
  ([config]
   (map->AuthorizationService {:config config})))
