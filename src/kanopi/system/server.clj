(ns kanopi.system.server
  (:require [com.stuartsierra.component :as component]
            [kanopi.model.storage.datomic :refer (datomic-peer)]
            [kanopi.model.data :refer (data-service)]
            [kanopi.model.session :refer (session-service)]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.controller.authorizer :as authorizer]
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
       (authenticator/new-authentication-service (with-dev :auth))
       {:database :datomic-peer})

      :session-service
      (component/using
       (session-service (with-dev :session))
       {:data-service  :data-service
        :datomic-peer  :datomic-peer
        :authenticator :authenticator})

      :authorizer
      (component/using
       (authorizer/new-authorization-service (with-dev :auth))
       {:database :datomic-peer})

      :web-app
      (component/using
       (app/new-web-app (with-dev :web-app))
       {:data-service    :data-service
        :session-service :session-service
        :authenticator   :authenticator})

      :web-server
      (component/using
       (server/new-web-server (with-dev :web-server))
       {:web-app :web-app})

      ))))
