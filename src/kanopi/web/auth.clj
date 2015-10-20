(ns kanopi.web.auth
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]
            [clojure.set :as set]
            [datomic.api :as d]
            [kanopi.data.impl :as data-impl]
            [kanopi.data :as data]
            [kanopi.storage.datomic :as datomic]
            [com.stuartsierra.component :as component]))

(defn authentication-middleware
  [handler credential-fn]
  (let [friend-m
        {
         :allow-anon? true
         :redirect-on-auth? false

         :credential-fn (partial creds/bcrypt-credential-fn credential-fn)
         ;; TODO: make better error handlers which return errors
         ;; described as data
         :login-failure-handler (fn [e]
                                  {:status 401})
         :unauthenticated-handler (constantly {:status 401})
         :unauthorized-handler    (fn [e]
                                    {:status 401})
         :login-uri "/login"
         :default-landing-uri "/"
         :workflows [
                     (workflows/interactive-form :redirect-on-auth? false :allow-anon? true)
                     (workflows/http-basic :realm "/")
                     ]}]
    (-> handler
        (friend/authenticate friend-m))))

(defprotocol IAuthenticate
  (credentials  [this username])
  (verify-creds [this input-creds])
  (register!    [this username password]))

(defn- valid-credentials? [creds]
  (and
   (integer? (:ent-id creds))
   (coll?    (:role creds))
   (every? (comp integer? :db/id) (:role creds))
   (string?  (:username creds))
   (string?  (:password creds))))

;; FIXME: refactor to use the data-service
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
                          {:user/role [:db/id :role/label]}
                          :user/id
                          :user/password]
                        ent-id) 
          creds' (when (not-empty (dissoc creds :db/id))
                   (hash-map
                    :ent-id   (get-in creds [:db/id])
                    :role     (get-in creds [:user/role])
                    :username (get-in creds [:user/id])
                    :password (get-in creds [:user/password])))]
      (when creds'
        (assert (valid-credentials? creds') "Invalid credential map."))
      creds'))

  (verify-creds [this {:keys [username password]}]
    (some->> (credentials this username)
             :password
             (creds/bcrypt-verify password)))

  (register! [this username password]
    ;; TODO: should this return nil when user already exists or throw
    ;; an exception? Currently it throws an exception.
    (assert (every? identity [username password]) "Missing username or password.")
    (assert (nil? (d/entid (datomic/db database nil) [:user/id username]))
            "This username is already taken. Please choose another.")
    ;; TODO: add audit datoms to the tx entity
    (let [user-ent-id  (d/tempid :db.part/user -1)
          user-role-id (d/tempid :db.part/users -1000)
          txdata [
                  [:db/add user-role-id :role/id username]
                  [:db/add user-role-id :role/label username]
                  [:db/add user-ent-id :user/id username]
                  [:db/add user-ent-id :user/password (creds/hash-bcrypt password)]
                  [:db/add user-ent-id :user/role user-role-id]
                  ]
          report @(datomic/transact database nil txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) user-ent-id)))

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

(defprotocol IAuthorize
  (authorized-methods [this creds ent-id])
  (authorized? [this creds request])
  (enforce-entitlements [this ]))

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
