(ns kanopi.dev.view.fact
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [quile.component :as component]
            [kanopi.dev.util :as dev-util]
            [kanopi.model.intro-data :as intro-data]
            [om.core :as om]
            [om.dom :as dom]
            [kanopi.view.fact :as fact]))

(defonce system
  (component/start (dev-util/new-system)))

(defcard view-fact-handle
  (dc/om-root fact/handle {:init-state {:mode :view
                                        :fact-hovering false
                                        :fact-count 1
                                        :datum-id -200}
                           :shared {:aether (get-in system [:aether :aether])}})
  {:db/id -100}
  {:inspect-data true, :history true})

(defcard view-fact-handle-hover
  (dc/om-root fact/handle {:init-state {:mode :view
                                        :fact-hovering true
                                        :fact-count 1
                                        :datum-id -200}
                           :shared {:aether (get-in system [:aether :aether])}})
  {:db/id -100}
  {:inspect-data true, :history true})

(defcard edit-fact-handle
  (dc/om-root fact/handle {:init-state {:mode :edit
                                        :fact-hovering false
                                        :fact-count 1
                                        :datum-id -200}
                           :shared {:aether (get-in system [:aether :aether])}})
  {:db/id -100}
  {:inspect-data true, :history true})

(defcard fact-container
  (dc/om-root fact/container {:init-state {:datum-id -200
                                           :fact-count 1}
                              :shared {:aether (get-in system [:aether :aether])}})
  (atom {:db/id -100})
  {:inspect-data true, :history true})
