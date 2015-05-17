(ns nebula.web.resources.spa
  (:require [liberator.core :refer [defresource]]
            [nebula.web.resources.base :as base]
            [nebula.web.resources.templates :as html]))

(defn spa [ctx]
  (html/om-page {:title "nebula"}))

(defresource spa-resource base/requires-authentication
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
