(ns kanopi.system
  (:require [com.stuartsierra.component :as component]
            [kanopi.storage.datomic :refer (datomic-peer)]
            [kanopi.data :refer (database)]
            [kanopi.web.auth :as auth]
            [kanopi.web.server :as server]
            [kanopi.web.app :as app]
            [environ.core :refer [env]]
            [kanopi.util.core :as util]
            ))

(defn new-system
  ([] (new-system env))
  ([config]
   (let [{:keys [port env]} config
         with-dev #(util/select-with-merge config % [:dev])]
    (component/system-map
     :datomic
     (datomic-peer "localhost" 4334 (with-dev :datomic))

     :database
     (component/using
      (database)
      {:database :datomic})


     :authenticator
     (component/using
      (auth/new-authentication-service)
      {:database :datomic})

     :web-app
     (component/using
      (app/new-web-app (with-dev :web-app))
      {:database :database
       :authenticator :authenticator})

     :web-server
     (component/using
      (server/new-web-server (with-dev :web-server))
      {:web-app :web-app})))))
