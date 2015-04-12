(ns nebula.web.spa
  (:require [liberator.core :refer [defresource]]
            [hiccup.page :refer [html5 include-js include-css]]))

(defn spa [ctx]
  (html5
   [:head
    [:title "nebula"]]
   [:body
    ]))

(defresource spa-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
