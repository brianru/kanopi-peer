(ns kanopi.aether.core-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [com.stuartsierra.component :as component]
            [cljs.test :refer-macros (testing is)]
            [kanopi.util-dev :as dev-util]))

(defonce system
  (component/start (dev-util/new-system)))

(deftest aether-core
  (let [aether (get-in system [:aether :aether])]
    (testing "aether core"
      (is (not-empty aether))
      (is (contains? aether :noun-publication))
      (is (contains? aether :verb-publication)))))

