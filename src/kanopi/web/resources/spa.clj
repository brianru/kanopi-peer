(ns kanopi.web.resources.spa
  (:require [liberator.core :refer [defresource]]
            [cemerick.friend :as friend]
            [kanopi.web.resources.base :as base]
            [kanopi.web.resources.templates :as html]))

(defn spa [ctx]
  (let []
    (html/om-page
     ctx
     {:title "kanopi"
      :cookie (hash-map
               :user (friend/current-authentication (:request ctx)))})
    ))

(defresource spa-resource base/requires-authentication
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
