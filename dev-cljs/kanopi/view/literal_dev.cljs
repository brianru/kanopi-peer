(ns kanopi.view.literal-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [cljs.test :refer-macros (is)]
            [com.stuartsierra.component :as component]
            [kanopi.util-dev :as dev-util]
            
            [kanopi.view.literal :as literal]))

(defonce system
  (component/start (dev-util/new-system)))

(defcard empty-literal
  (dc/om-root literal/container {:shared (dev-util/shared-state system)})
  {}
  {:inspect-data true, :history true}
  )

(defcard text-literal
  (dc/om-root literal/container {:shared (dev-util/shared-state system)})
  {}
  {:inspect-data true, :history true}
  )
