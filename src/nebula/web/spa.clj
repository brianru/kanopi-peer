(ns nebula.web.spa
  (:require [liberator.core :refer [defresource]]
            [hiccup.page :refer [html5 include-js include-css]]))

(defn spa [ctx]
  (html5
   [:head
    [:title "nebula"]
    [:link {:rel "icon"
            :type "image/png"
            :href "/favicon.png"}]
    (include-css "/css/main.css")]

   [:body
    [:div#app-container
     (include-js "js/main.js")]]))

(defresource spa-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
