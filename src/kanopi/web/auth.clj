(ns kanopi.web.auth
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]
            [clojure.set :as set]
            [datomic.api :as d]
            [kanopi.data :as data]
            [kanopi.storage.datomic :as datomic]
            [com.stuartsierra.component :as component]))

(defn authentication-middleware
  [handler credential-fn]
  (let [friend-m
        {:credential-fn (partial creds/bcrypt-credential-fn credential-fn)
         ;; TODO: define unauthorized handler.
         ;; :unauthorized-handler {:status 403 :body ___}
         :login-uri "/login"
         :default-landing-uri "/"
         :workflows [(workflows/http-basic :realm "/")
                     (workflows/interactive-form)]}]
    (-> handler
        (friend/authenticate friend-m))))

(defprotocol IAuthenticate
  (credentials  [this username])
  (verify-creds [this input-creds])
  (register!    [this username password]))

;; FIXME: refactor to use real database component with its own query api
(defrecord AuthenticationService [config database user-lookup-fn]

  IAuthenticate

  (credentials [this username]
    (let [db     (datomic/db database nil)
          ent-id (d/entid db [:user/id username])
          creds  (->> ent-id (d/entity db) (into {}))]
      {:ent-id   ent-id
       :role     (-> creds :user/role first :db/id)
       :username (:user/id creds)
       :password (:user/password creds)}))

  (verify-creds [this {:keys [username password]}]
    (->> (credentials this username)
         :user/password
         (creds/bcrypt-verify password)))

  (register! [this username password]
    ;; TODO: should this return nil when user already exists or throw
    ;; an exception?
    (assert (nil? (d/entid (datomic/db database nil) [:user/id username]))
            "This username is already taken. Please choose another.")
    (let [user-ent-id (d/tempid :db.part/user -1)
          txdata [
                  {:db/id #db/id [:db.part/user -1000]
                   :role/id username}
                  {:db/id user-ent-id
                   :user/id username
                   :user/password (creds/hash-bcrypt password)
                   :user/role #db/id [:db.part/user -1000]}
                  ]
          report @(datomic/transact database nil txdata)]
      (d/resolve-tempid (:db-after report) (:tempids report) user-ent-id)))

  component/Lifecycle

  (start [this]
    (assoc this :credential-fn (partial credentials this)))

  (stop [this]
    (assoc this :credential-fn nil)))

(defn new-authentication-service
  "Helper fn."
  ([]
   (new-authentication-service {}))
  ([config]
   (map->AuthenticationService {:config config})))

(defprotocol IAuthorize
  (authorized-methods [this cur-auth ent-id])
  (authorized? [this cur-auth request]))

(defrecord AuthorizationService
    [config database authorized?]
  IAuthorize
  (authorized-methods [this cur-auth ent-id]
    #{:get :put :post :delete})
  (authorized? [this cur-auth request]
    (let [ent-id nil]
      (contains?
       (authorized-methods this cur-auth ent-id)
       (:request-method request)))))

(defn new-authorization-service
  "Helper fn."
  ([]
   (new-authorization-service {}))
  ([config]
   (map->AuthorizationService {:config config})))
