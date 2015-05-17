(ns nebula.system
  (:require [com.stuartsierra.component :as component]
            [nebula.storage.datomic :refer [database]]
            [nebula.web.server :as server]
            [nebula.web.app :as app]
            [environ.core :refer [env]]
            [nebula.util.core :as util]
            ))

(defn new-system
  ([] (new-system env))
  ([config]
   (let [{:keys [port env]} config
         with-dev #(util/select-with-merge config % [:dev])]
    (component/system-map
     :database   (database "localhost" 4334 (with-dev :datomic))
     :web-app    (component/using
                  (app/new-web-app (with-dev :web-app))
                  {:database :database})
     :web-server (component/using
                  (server/new-web-server (with-dev :web-server))
                  {:web-app :web-app})))))
