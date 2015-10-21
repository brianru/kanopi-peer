(ns kanopi.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [kanopi.system :refer [new-system]]
            [kanopi.web.routes :as routes]
            [environ.core :refer [env]]))

(def default-config
  {:web-server {:port    (or (env :web-server-port) 8080)
                :host    (or (env :web-server-host) "0.0.0.0")}
   :web-app    {:handler #'routes/app-routes}
   :datomic    {:uri     (or (env :datomic-transactor-env-datomic-transactor-uri)
                             (env :datomic-transactor-uri)
                             "datomic:mem://")
                :db-name (or (env :datomic-database-env-database-name)
                             (env :datomic-database-name)
                             "kanopi42")
                :schema  ["resources/schema.edn"]
                :data    ["resources/test-data.edn"
                          "resources/init-data.edn"]}
   :auth       {:init-user-data ["resources/test-data.edn"]}
   :dev true})

(defn system-config
  "For some reason I feel an extra layer of indirection may come in
  handy. Maybe this should take some arguments."
  []
  default-config)

(defn -main [& args]
  "Default application entry point."
  (let [sys (new-system default-config)]
    (println "starting system")
    (clojure.pprint/pprint env)
    (component/start sys)))
