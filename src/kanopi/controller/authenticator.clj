(ns kanopi.controller.authenticator
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [schema.core :as s]
            [cemerick.friend.credentials :as creds]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]))

(defprotocol IAuthenticate
  (register!    [this username password])
  (credentials  [this username])
  (verify-creds [this input-creds]
                [this username password])
  (change-password! [this username current-password new-password]))

(defn valid-credentials? [creds]
  (s/validate schema/Credentials creds))

(defrecord AuthenticationService [config database user-lookup-fn]

  IAuthenticate

  (credentials [this username]
    (let [db     (datomic/db database nil)
          ent-id (-> (d/entity db [:user/id username]) :db/id)
          ;; NOTE: this pull call and the following when/not-empty
          ;; check belong in a lower-level ns. see kanopi.data vs
          ;; kanopi.data.impl as example
          creds  (d/pull db
                        '[:db/id
                          {:user/team [:db/id :team/id]}
                          :user/id
                          :user/password]
                        ent-id) 
          ;; NOTE: the shape of creds' is largely dictated by friend
          creds' (when (not-empty (dissoc creds :db/id))
                   (hash-map
                    :ent-id   (get-in creds [:db/id])
                    :username (get-in creds [:user/id])
                    :password (get-in creds [:user/password])
                    :teams    (get-in creds [:user/team])
                    ;; NOTE: DEFAULT CURRENT TEAM is the user's
                    ;; personal team. this should be changeable.
                    :current-team (->> (get-in creds [:user/team])
                                       (filter #(= (:user/id creds)
                                                   (:team/id %)))
                                       (first))
                    ))]
      (when creds'
        (assert (s/validate schema/Credentials creds') "Invalid credential map."))
      creds'))

  (verify-creds [this {:keys [username password]}]
    (verify-creds this username password))
  (verify-creds [this username password]
    (some->> (credentials this username)
             :password
             (creds/bcrypt-verify password)))

  (register! [this username password]
    ;; TODO: should this return nil when user already exists or throw
    ;; an exception? Currently it throws an exception.
    (assert (s/validate schema/InputCredentials [username password])
            "Invalid username or password")
    (assert (nil? (d/entid (datomic/db database nil) [:user/id username]))
            "This username is already taken. Please choose another.")
    ;; TODO: add audit datoms to the tx entity
    (let [user-ent-id    (d/tempid :db.part/user -1)
          user-team-id   (d/tempid :db.part/users -1000)
          init-user-data (some-> (get config :init-user-data)
                                 (slurp)
                                 (read-string)
                                 (->> (map (fn [ent]
                                             (case (schema/describe-entity ent)
                                               :datum
                                               (assoc ent :datum/team user-team-id)
                                               :literal
                                               (assoc ent :literal/team user-team-id)
                                               ;; default
                                               ent)))))
          txdata (concat
                  [
                   [:db/add user-team-id :team/id username]
                   [:db/add user-ent-id  :user/id username]
                   [:db/add user-ent-id  :user/password (creds/hash-bcrypt password)]
                   [:db/add user-ent-id  :user/team user-team-id]
                   ]
                  init-user-data
                  ) 
          report @(datomic/transact database nil txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) user-ent-id)))

  (change-password! [this username current-password new-password]
    (assert (and (not= current-password new-password)
                 (s/validate schema/UserPassword new-password))
            "New password is invalid")
    (assert (verify-creds this username current-password)
            "Current password is incorrect")
    (let [{user-ent-id :ent-id :as creds} (credentials this username)
          txdata [
                  [:db/retract user-ent-id :user/password current-password]
                  [:db/add     user-ent-id :user/password (creds/hash-bcrypt new-password)]
                  ]
          report @(datomic/transact database creds txdata)]
      (verify-creds this username new-password)))

  component/Lifecycle

  (start [this]
    (assoc this :user-lookup-fn (partial credentials this)))

  (stop [this]
    (assoc this :user-lookup-fn nil)))

(defn new-authentication-service
  "Helper fn."
  ([]
   (new-authentication-service {}))
  ([config]
   (map->AuthenticationService {:config config})))
