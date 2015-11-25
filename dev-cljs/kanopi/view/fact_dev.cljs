(ns kanopi.view.fact-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [cljs.test :refer-macros (is)]
            [com.stuartsierra.component :as component]
            [kanopi.util-dev :as dev-util]
            [om.core :as om]
            [om.dom :as dom]
            [kanopi.view.fact :as fact]
            ))

(defonce system
  (component/start (dev-util/new-system)))

(defcard fact-docs
  "Facts work even when props are empty. Single component for
  initialization viewing and editing.
  ")

(defcard empty-fact
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id -42
                                           :editing nil}
                              :state {:fact-count 0}})
  {:inspect-data true, :history true})

(defcard empty-fact-editing
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id 42
                                           :editing :attribute}
                              :state {:fact-count 0}}))

(defcard view-existing-fact
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id 42}
                              :state {:fact-count 1}})
  {:db/id 710
   :fact/attribute {:db/id 810
                    :literal/text "pattern"}
   :fact/value     {:db/id 901
                    :datum/label "lantern in the fog"}})

(defcard edit-existing-fact-attribute
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id 42
                                           :editing :attribute}
                              :state {:fact-count 1}})
  {:db/id 710
   :fact/attribute {:db/id 810
                    :literal/text "pattern"}
   :fact/value     {:db/id 901
                    :datum/label "lantern in the fog"}})

(defcard edit-existing-fact-value
  (dc/om-root fact/fact-next {:shared (dev-util/shared-state system)
                              :init-state {:datum-id 42
                                           :editing :value}
                              :state {:fact-count 1}})
  {:db/id 710
   :fact/attribute {:db/id 810
                    :literal/text "pattern"}
   :fact/value     {:db/id 901
                    :datum/label "lantern in the fog"}})

(deftest correct-handle-states
  (is (fact/handle-fill :empty nil)          "red")
  (is (fact/handle-fill :empty :attribute)   "red")
  (is (fact/handle-fill :partial nil)        "red")
  
  (is (fact/handle-fill :partial :attribute) "yellow")
  (is (fact/handle-fill :partial :value)     "yellow")

  (is (fact/handle-fill :complete :attribute) "green")

  (is (fact/handle-fill :complete nil) ""))
