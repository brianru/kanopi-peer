(ns kanopi.controller.system
  (:require [com.stuartsierra.component :as component]
            [kanopi.model.storage.datomic :refer (datomic-peer)]
            [kanopi.model.data :refer (data-service)]
            [kanopi.web.auth :as auth]
            [kanopi.controller.web-server :as server]
            [kanopi.view.web-app :as app]
            [environ.core :refer [env]]
            [kanopi.util.core :as util]
            ))

(defn new-system
  ([] (new-system env))
  ([config]
   (let [{:keys [port env]} config
         with-dev #(util/select-with-merge config % [:dev])]
     (println "New System")
     (println "Configuration:" config)
    (component/system-map
     :datomic-peer
     (datomic-peer (with-dev :datomic))

     :data-service
     (component/using
      (data-service)
      {:datomic-peer :datomic-peer})

     :authenticator
     (component/using
      (auth/new-authentication-service (with-dev :auth))
      {:database :datomic-peer})

     :authorizer
     (component/using
      (auth/new-authorization-service (with-dev :auth))
      {:database :datomic-peer})

     :web-app
     (component/using
      (app/new-web-app (with-dev :web-app))
      {:data-service :data-service
       :authenticator :authenticator})

     :web-server
     (component/using
      (server/new-web-server (with-dev :web-server))
      {:web-app :web-app})
     
     ))))
