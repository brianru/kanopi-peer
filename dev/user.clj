(ns user
  (:require [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [environ.core :refer [env]]
            [nebula.system :refer [new-system]]
            [nebula.main :refer [default-config]]

            [nebula.generators :as ngen]
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
  (require '[nebula.web.auth :as auth])

  (auth/register! (get system :authenticator) "brian" "rubinton")

  (get system :authenticator)
  )
