(ns user
  (:gen-class)
  (:require [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [environ.core :refer [env]]
            [kanopi.system.server :as server]
            [kanopi.main]
            [datomic.api :as d]))


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

  (d/q '[:find [?t ?u ?uid] :in $ :where [?tx :tx/byUser ?u] [?u :user/id ?uid] [?tx :db/txInstant ?t]] db)

  (def x
    (println
     (d/q '[:find ?user ?user-ent-id ?time ?tx
            :in $ ?user-ent-id
            :where
            [?tx :tx/byUser ?user-ent-id]
            [?user-ent-id :user/id ?user]
            [?tx :db/txInstant ?time]
            (or [?e :datum/label _ ?tx]
                [?e :datum/fact _ ?tx])
            ;; constrain the entities modified to those with a
            ;; :datum/team, which may have been modified in a separate tx
            ]
          db 277076930200588))

    )

  (d/q '[:find ?tx :in $ :where [?tx :tx/byUser _ _]]
       db)


  x

  (d/pull db '[*] x)

  )
