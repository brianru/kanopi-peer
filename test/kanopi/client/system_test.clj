(ns kanopi.client.system-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            [schema.core :as s]

            [kanopi.model.schema :as schema]
            
            [kanopi.system.client :as client]))

(deftest create-system
  (let [system (component/start (client/new-system {}))]
    (is (not-empty system))
    (is (not (s/check schema/AppState @(get-in system [:app-state :app-state]))))
    
    (component/stop system)))
