(ns kanopi.controller.authenticator
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [schema.core :as s]
            [cemerick.friend.credentials :as creds]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.util.core :as util]
            [clojure.java.io :as io]))


(defprotocol IAuthenticate
  (-init-user-data [this username password user-ent-id user-team-id]
                   "Datoms asserting the user and team entities.")
  (-init-team-data [this team-id]
                   "Add team datoms to raw data.")
  (register!    [this username password])
  (credentials  [this username])
  (verify-creds [this input-creds]
                [this username password])
  (change-password! [this username current-password new-password]
                    [this username current-password new-password confirm-new-password]))

(defn valid-credentials? [creds]
  (s/validate schema/Credentials creds))

(defn credentials*
  ([db username]
   (credentials* db username true))
  ([db username validate?]
   (let [ent-id (-> (d/entity db [:user/id username]) :db/id)
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
     (println "YOYO" creds')
     (when (and validate? creds')
       (assert (s/validate schema/Credentials creds') "Invalid credential map."))
     creds')))

(defrecord AuthenticationService [config init-data database user-lookup-fn]

  IAuthenticate

  (credentials [this username]
    (println "CREDS" username)
    (credentials* (datomic/db database nil) username))

  (verify-creds [this {:keys [username password]}]
    (verify-creds this username password))
  (verify-creds [this username password]
    (println "verify creds" username password)
    (some->> (credentials this username)
             :password
             (creds/bcrypt-verify password)))

  (-init-team-data [this team-id]
    (map (fn [ent]
           (case (schema/describe-entity ent)
             :datum
             (assoc ent :datum/team team-id)
             :literal
             (assoc ent :literal/team team-id)
             ;; default
             ent))
         init-data))

  (-init-user-data [this username password user-temp-id team-temp-id]
    (vector
     [:db/add team-temp-id :team/id username]
     [:db/add user-temp-id :user/id username]
     [:db/add user-temp-id :user/password (creds/hash-bcrypt password)]
     [:db/add user-temp-id :user/team team-temp-id]))

  (register! [this username password]
    (println "HEREHERE" username password)
    ;; TODO: should this return nil when user already exists or throw
    ;; an exception? Currently it throws an exception.
    (assert (s/validate schema/InputCredentials [username password])
            "Invalid username or password")
    (assert (nil? (d/entid (datomic/db database nil) [:user/id username]))
            "This username is already taken. Please choose another.")
    ;; TODO: add audit datoms to the tx entity
    (let [user-temp-id (d/tempid :db.part/users -1)
          team-temp-id (d/tempid :db.part/users -1000)
          txdata
          (->> (concat
                (-init-user-data this username password user-temp-id team-temp-id)
                (-init-team-data this team-temp-id))
               (remove nil?))
          report       @(datomic/transact database nil txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) user-temp-id)))

  ;; Confirm new password arity
  (change-password! [this username current-password new-password confirm-new-password]
    (when (= new-password confirm-new-password)
      (change-password! this username current-password new-password)))
  ;; Change password arity, after confirmation
  (change-password! [this username current-password new-password]
    (assert (and (not= current-password new-password)
                 (s/validate schema/UserPassword new-password))
            "New password is invalid")
    (assert (verify-creds this username current-password)
            "Current password is incorrect")
    (let [{user-ent-id :ent-id :as creds} (credentials this username)
          txdata [[:db/retract user-ent-id :user/password current-password]
                  [:db/add     user-ent-id :user/password (creds/hash-bcrypt new-password)]]
          report @(datomic/transact database creds txdata)]
      (verify-creds this username new-password)))

  component/Lifecycle
  (start [this]
    (let [init-data (some-> (get config :init-user-data)
                            (io/resource)
                            (slurp)
                            (read-string))]
      (assoc this
             :user-lookup-fn (partial credentials this)
             :init-data init-data)))

  (stop [this]
    (assoc this :user-lookup-fn nil)))

(defn new-authentication-service
  "Helper fn."
  ([]
   (new-authentication-service {}))
  ([config]
   (map->AuthenticationService {:config config})))
