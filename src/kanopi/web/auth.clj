(ns kanopi.web.auth
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]
            [clojure.set :as set]
            [datomic.api :as d]
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
(defrecord AuthenticationService
    [config database user-lookup-fn]
  IAuthenticate
  (credentials [this username]
    (let [creds (->> [:user/id username]
                     (d/entity (d/db (:connection database)))
                     (into {}))
          ]
      {:username (:user/id creds)
       :password (:user/password creds)})
    )

  (verify-creds [this {:keys [username password]}]
    (->> (credentials this username)
         :user/password
         (creds/bcrypt-verify password)))

  (register! [this username password]
    (let [txdata [
                  {:db/id #db/id [:db.part/user -1000]
                   :role/id username}
                  {:db/id #db/id [:db.part/user]
                   :user/id username
                   :user/password (creds/hash-bcrypt password)
                   :user/role #db/id [:db.part/user -1000]}
                  ]
          ]
      (d/transact (:connection database) txdata)))

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
