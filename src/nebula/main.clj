(ns nebula.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [nebula.system :refer [new-system]]
            [environ.core :refer [env]]))

(def default-config
  {:web {}
   :datomic {}})

(defn -main [& args]
  (let [sys (new-system default-config)]
    (println "starting system")
    (component/start sys)))
