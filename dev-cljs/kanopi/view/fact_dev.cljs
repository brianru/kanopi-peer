(ns kanopi.view.fact-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [cljs.test :refer-macros (is)]
            [quile.component :as component]
            [kanopi.util-dev :as dev-util]
            [om.core :as om]
            [om.dom :as dom]
            [kanopi.view.fact :as fact]
            ))

(defonce system
  (component/start (dev-util/new-system)))

(defcard empty-fact
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id -42
                                           :mode :empty
                                           :editing nil}
                              :state {:fact-count 0}})
  {}
  {:inspect-data true, :history true})

(defcard empty-fact-editing
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id 42
                                           :mode :empty
                                           :editing :attribute}
                              :state {:fact-count 0}}))

(deftest correct-handle-states
  (is (fact/handle-fill :empty nil)          "red")
  (is (fact/handle-fill :empty :attribute)   "red")
  (is (fact/handle-fill :partial nil)        "red")
  
  (is (fact/handle-fill :partial :attribute) "yellow")
  (is (fact/handle-fill :partial :value)     "yellow")

  (is (fact/handle-fill :complete :attribute) "green")

  (is (fact/handle-fill :complete nil) ""))
