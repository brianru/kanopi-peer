(ns kanopi.view.header-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [om.core :as om]
            [quile.component :as component]
            [kanopi.view.header :as header]
            [kanopi.util-dev :as dev-util]
            ))

(defonce system
  (component/start (dev-util/new-system)))

(defcard team-selector-no-user
  (dc/om-root header/left-team-dropdown {:shared (dev-util/shared-state system)})
  {:user {}})

(defcard team-selector-one-team
  (dc/om-root header/left-team-dropdown {:shared (dev-util/shared-state system)})
  (let [team {:db/id 20 :team/id "brian"}]
    {:user {:ent-id 42
            :username "brian"
            :current-team team
            :teams [team]}}))

(defcard team-selector-three-teams
  (dc/om-root header/left-team-dropdown {:shared (dev-util/shared-state system)})
  (let [teams [{:db/id 20 :team/id "brian"}
               {:db/id 21 :team/id "hannah"}
               {:db/id 22 :team/id "applebottom"}]]
    {:user {:ent-id 42
            :username "brian"
            :current-team (first teams)
            :teams teams}}))

(defcard controls-anonymous
  (dc/om-root header/right-controls {:shared (dev-util/shared-state system)})
  {:user {}})

(defcard controls-authenticated
  (dc/om-root header/right-controls {:shared (dev-util/shared-state system)})
  (let [team {:db/id 22 :team/id "brian"}]
    {:user {:ent-id 42
            :identity "brian"
            :username "brian"
            :current-team team
            :teams [team]}}))
