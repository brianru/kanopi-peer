(ns nebula.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [nebula.system :refer [new-system]]
            [nebula.web.routes :as routes]
            [environ.core :refer [env]]))

(def default-config
  {:web-server {:port    8080
                :host    "0.0.0.0"}
   :web-app    {:handler #'routes/app-routes}
   :datomic    {:schema  ["resources/schema.edn"]
                :data    []}
   :dev true})

(defn -main [& args]
  "Default application entry point."
  (let [sys (new-system default-config)]
    (println "starting system")
    (component/start sys)))
