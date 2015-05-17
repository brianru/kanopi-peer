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

(defrecord AuthenticationService
    [config database user-lookup-fn]
  component/Lifecycle
  (start [this]
    (assoc this :user-lookup-fn (constantly nil)))
  (stop [this]
    (dissoc this :user-lookup-fn)))

(defn new-authentication-service
  ([]
   (new-authentication-service {}))
  ([config]
   (map->AuthenticationService {:config config})))
