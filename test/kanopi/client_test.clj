(ns kanopi.client-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            
            [kanopi.system.client :as client]))

(deftest create-system
  (let [system (component/start (client/new-system {}))]
    
    (component/stop system)))
