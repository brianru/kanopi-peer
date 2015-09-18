(ns kanopi.web.resources.spa
  (:require [liberator.core :refer [defresource]]
            [cemerick.friend :as friend]
            [kanopi.web.resources.base :as base]
            [kanopi.web.resources.templates :as html]))

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
