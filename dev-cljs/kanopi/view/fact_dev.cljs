(ns kanopi.view.fact-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [quile.component :as component]
            [kanopi.util-dev :as dev-util]
            [om.core :as om]
            [om.dom :as dom]
            [kanopi.view.fact :as fact]
            ))

(js/alert 40)

(defonce system
  (component/start (dev-util/new-system)))

(defcard view-fact-handle
  (dc/om-root fact/handle {:init-state {:mode :view
                                        :fact-hovering false
                                        :fact-count 1
                                        :datum-id -200}
                           :shared (dev-util/shared-state system)})
  {:db/id -100}
  {:inspect-data true, :history true})

(defcard view-fact-handle-hover
  (dc/om-root fact/handle {:init-state {:mode :view
                                        :fact-hovering true
                                        :fact-count 1
                                        :datum-id -201}
                           :shared (dev-util/shared-state system)})
  {:db/id -101}
  {:inspect-data true, :history true})

(defcard edit-fact-handle
  (dc/om-root fact/handle {:init-state {:mode :edit
                                        :fact-hovering false
                                        :fact-count 1
                                        :datum-id -202}
                           :shared (dev-util/shared-state system)})
  {:db/id -102}
  {:inspect-data true, :history true})

(defcard empty-fact-container
  (dc/om-root fact/container {:init-state {:datum-id -203
                                           :fact-count 1}
                              :shared (dev-util/shared-state system)})
  (atom {:db/id -103})
  {:inspect-data true, :history true})

(defcard fact-missing-value
  (dc/om-root fact/container {:init-state {:datum-id -204
                                           :fact-count 1}
                              :shared (dev-util/shared-state system)})
  (atom {:db/id -104
         :fact/attribute {:db/id -204
                          :literal/text "I am an attribute!"}
         :fact/value nil
         })
  {:inspect-data true, :history true})

(defcard fact-missing-attribute
  (dc/om-root fact/container {:init-state {:datum-id -204
                                           :fact-count 1
                                           :mode :view}
                              :shared (dev-util/shared-state system)})
  (atom {:db/id -104
         :fact/attribute {}
         :fact/value {:db/id -204
                      :literal/text "I am a value!"}
         })
  {:inspect-data true, :history true})

(defcard fact-view-mode
  (dc/om-root fact/container {:init-state {:datum-id -204
                                           :fact-count 1
                                           :mode :view}
                              :shared (dev-util/shared-state system)})
  (atom {:db/id -104
         :fact/attribute {}
         :fact/value {}
         })
  {:inspect-data true, :history true})

(defcard fact-edit-mode
  (dc/om-root fact/container {:init-state {:datum-id -204
                                           :fact-count 1
                                           :mode :edit}
                              :shared (dev-util/shared-state system)})
  (atom {:db/id -104
         :fact/attribute {}
         :fact/value {}
         })
  {:inspect-data true, :history true})
