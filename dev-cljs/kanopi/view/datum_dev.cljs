(ns kanopi.view.datum-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [quile.component :as component]
            [kanopi.util-dev :as dev-util]
            [om.core :as om]
            [kanopi.view.datum :as datum]))

(defonce system
  (component/start (dev-util/new-system)))

(defcard view-empty-datum
  (dc/om-root datum/body {:shared (dev-util/shared-state system)})
  {}
  {:inspect-data true, :history true})

(defcard view-init-datum
  (dc/om-root datum/body {:shared (dev-util/shared-state system)})
  {:db/id -1
   :datum/label ""
   :datum/fact []}
  {:inspect-data true, :history true})

(defcard view-datum-one-fact
  (dc/om-root datum/body {:shared (dev-util/shared-state system)})
  {:db/id 20
   :datum/label "Kanopi Pattern Language"
   :datum/fact [{:db/id 22
                 :fact/attribute {:db/id 30
                                  :literal/text "take me to"}
                 :fact/value {:db/id 31
                              :literal/text "funky town"}}
                ]}
  {:inspect-data true, :history true})

(defcard view-datum-three-facts
  (dc/om-root datum/body {:shared (dev-util/shared-state system)})
  {}
  {:inspect-data true, :history true})

(defcard view-datum-thirty-facts
  (dc/om-root datum/body {:shared (dev-util/shared-state system)})
  {}
  {:inspect-data true, :history true})

(defcard edit-datum
  "TODO")
