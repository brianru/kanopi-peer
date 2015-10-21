(ns kanopi.dev.aether.core
  (:require-macros [devcards.core :as dc :refer (defcard deftest)]
                   [cljs.test :refer (testing is)])
  (:require [quile.component :as component]
            [kanopi.dev.util :as dev-util]))

(defonce system
  (component/start (dev-util/new-system)))

(deftest aether-core
  (let [aether (get-in system [:aether :aether])]
    (testing "aether core"
      (is (not-empty aether))
      (is (contains? aether :noun-publication))
      (is (contains? aether :verb-publication)))))

