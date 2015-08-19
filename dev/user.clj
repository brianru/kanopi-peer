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

 (def datomic-peer (component/start
                    (dp/datomic-peer "localhost" 4334
                                     {:schema ["resources/schema.edn"]
                                      :data ["resources/test-data.edn"
                                             "resources/init-data.edn"]})))

 (def db (dp/db datomic-peer nil))
 (d/q '[:find [?ident ...] :where [_ :db/ident ?ident]] db)
 (component/stop datomic-peer)

 )
