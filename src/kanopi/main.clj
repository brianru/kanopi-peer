(ns kanopi.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [kanopi.system :refer [new-system]]
            [kanopi.web.routes :as routes]
            [environ.core :refer [env]]))

(def default-config
  {:web-server {:port    8080
                :host    "0.0.0.0"}
   :web-app    {:handler #'routes/app-routes}
   :datomic    {:uri     "datomic:mem://kanopi"
                :schema  ["resources/schema.edn"]
                :data    ["resources/test-data.edn"
                          "resources/init-data.edn"]}
   :dev true})

(defn -main [& args]
  "Default application entry point."
  (let [sys (new-system default-config)]
    (println "starting system")
    (component/start sys)))
