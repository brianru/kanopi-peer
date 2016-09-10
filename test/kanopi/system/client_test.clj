(ns kanopi.system.client-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            [schema.core :as s]

            [kanopi.model.schema :as schema]
            [kanopi.model.message :as message]

            [kanopi.controller.dispatch :as dispatch]

            [kanopi.system.client :as client]))

(deftest create-system
  (let [system (component/start (client/new-system {}))]
    (is (not-empty system))
    (println "HEREHERE"
             (get-in system [:app-state :app-state]))
    (is (not (s/check schema/AppState @(get-in system [:app-state :app-state]))))

    (component/stop system)))

(deftest create-and-get-datum
  (let [{:keys [dispatcher] :as system}
        (component/start (client/new-system {}))
        app-state (get-in system [:app-state :app-state])]
    (is (not-empty (get-in @app-state [:user])))
    (dispatch/transmit! dispatcher (message/create-datum))
    (is (not-empty (get-in @app-state [:datum])))
    (is (not (s/check schema/CurrentDatum (get-in @app-state [:datum]))))

    (let [current-datum (get-in @app-state [:datum])]
      (dispatch/transmit! dispatcher (message/get-datum (get-in current-datum [:datum :db/id])))
      ;; FIXME: datum in app state is missing a team
      ;; It's because the team's id is not in the cache.
      ;; `init-anonymous-session' is only putting the welcome ent id in cache.
      ;; how does the cache get populated here?
      (is (= current-datum (get-in @app-state [:datum]))))
    (component/stop system)))
