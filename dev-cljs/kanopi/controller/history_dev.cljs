(ns kanopi.controller.history-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)]
                   [cljs.test :refer (testing is)])
  (:require [com.stuartsierra.component :as component]
            [kanopi.util-dev :as dev-util]
            
            [kanopi.controller.history :as history]))

(defonce system
  (component/start (dev-util/new-system)))

(defcard history-examples
  (let [hist (get system :history)]
    (history/get-route-for hist [:datum :id 92])))

(deftest history
  (let [hist (get system :history)
        match (history/get-route-for hist [:datum :id 92])]
    (is (nil? match))))
