(ns nebula.web.auth
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]
            [com.stuartsierra.component :as component]))

(defn authentication-middleware
  [handler credential-lookup-fn]
  (let [friend-m
        {:credential-fn (partial creds/bcrypt-credential-fn credential-lookup-fn)
         :login-uri "/login"
         :default-landing-uri "/"
         :workflows [(workflows/http-basic :realm "/")
                     (workflows/interactive-form)]}]
    (-> handler
        (friend/authenticate friend-m))))

(def user-map
  "dummy in-memory user database."
  {"root" {:username "root"
           :password (creds/hash-bcrypt "admin_password")
           :roles #{:admin}}
   "jane" {:username "jane"
           :password (creds/hash-bcrypt "user_password")
           :roles #{:user}}})

(defrecord AuthenticationService
    [config database user-lookup-fn]
  component/Lifecycle
  (start [this]
    (assoc this :user-lookup-fn user-map))
  (stop [this]
    (assoc this :user-lookup-fn nil)))

(defn new-authentication-service
  ([]
   (new-authentication-service {}))
  ([config]
   (map->AuthenticationService {:config config})))
