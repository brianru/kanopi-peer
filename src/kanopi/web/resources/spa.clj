(ns kanopi.web.resources.spa
  (:require [liberator.core :refer [defresource]]
            [kanopi.web.resources.base :as base]
            [kanopi.web.resources.templates :as html]))

(defn spa [ctx]
  (html/om-page ctx {:title "kanopi"}))

(defresource spa-resource base/requires-authentication
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
