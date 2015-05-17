(ns nebula.web.spa
  (:require [liberator.core :refer [defresource]]
            [nebula.web.resources.templates :as html]))

(defn spa [ctx]
  (html/page {:title "nebula"} (html/include-om)))

(defresource spa-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
