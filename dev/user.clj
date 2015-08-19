(ns user
  (:require [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [environ.core :refer [env]]
            [kanopi.system :refer [new-system]]
            [kanopi.main :refer [default-config]]

            [kanopi.generators :as ngen]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            
            [kanopi.web.auth :as auth]
            [kanopi.data :as data]
            [kanopi.storage.datomic :as datomic]))

(defonce system nil)

(defn init []
  (alter-var-root #'system (constantly (new-system default-config))))

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

 (let [creds (auth/register! (:authenticator system) "rian" "banan")
       ent-id (data/init-thunk (:data-service system) creds)]
   (data/get-thunk (:data-service system) creds ent-id))

 (data/init-thunk datomic-peer nil)

 (component/stop system)

 )
