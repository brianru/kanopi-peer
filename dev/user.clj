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
            [clojure.test.check.properties :as prop]))

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
 (require '[kanopi.storage.datomic :as dp])
 (require '[kanopi.web.auth :as auth])
 (require '[kanopi.data :as data])

 (go)
 (reset)
 (let [creds (do @(auth/register! (:authenticator system) "brian" "rubinton")
                 (auth/credentials (:authenticator system) "brian"))
       res (data/init-thunk (:data-service system) creds)]
   res)

 (def dp (dp/db datomic-peer nil))
 (d/q '[:find [?ident ...] :where [_ :db/ident ?ident]] db)
 (data/init-thunk datomic-peer nil)
 (component/stop system)

 )
