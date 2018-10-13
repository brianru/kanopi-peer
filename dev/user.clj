(ns user
  (:require [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [environ.core :refer [env]]
            [kanopi.system.server :as server]
            [kanopi.main]))

(defonce system nil)

(defn init []
  (alter-var-root #'system (constantly (server/new-system (kanopi.main/system-config)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn go [] (init) (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(comment
 (go)
 (reset)

 (require '[datomic.api :as d])

 (def db (d/db (get-in system [:datomic-peer :connection])))

 (def x
   (ffirst
    (d/q '[:find ?e ?v :where [?e :datum/label ?v]] db)))

 x

 (d/pull db '[*] x)

 )
