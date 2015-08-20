(ns kanopi.system
  (:require [com.stuartsierra.component :as component]
            [kanopi.storage.datomic :refer (datomic-peer)]
            [kanopi.data :refer (data-service)]
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
     :datomic-peer
     (datomic-peer (with-dev :datomic))

     :data-service
     (component/using
      (data-service)
      {:datomic-peer :datomic-peer})

     :authenticator
     (component/using
      (auth/new-authentication-service)
      {:database :datomic-peer})

     :authorizer
     (component/using
      (auth/new-authorization-service)
      {:database :datomic-peer})

;;     :web-app
;;     (component/using
;;      (app/new-web-app (with-dev :web-app))
;;      {:database :data-service
;;       :authenticator :authenticator})
;;
;;     :web-server
;;     (component/using
;;      (server/new-web-server (with-dev :web-server))
;;      {:web-app :web-app})
     
     ))))
