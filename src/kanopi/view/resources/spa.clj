(ns kanopi.view.resources.spa
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :as rep]
            [cemerick.friend :as friend]
            [kanopi.view.resources.base :as base]
            [kanopi.view.resources.templates :as html]))

(defn spa [ctx]
  (let [user-data (friend/current-authentication (:request ctx))
        init-data nil]
    (html/om-page
     ctx
     {:title "kanopi"
      :cookie (hash-map
               :user user-data)})
    ))

(defresource spa-resource ;base/requires-authentication
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
