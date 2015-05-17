(ns nebula.web.resources.templates
  (:require [hiccup.page :refer (html5 include-js include-css)]
            [cemerick.friend :as friend]))

(defn include-bootstrap []
  (include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css"))

(defn include-om []
  (include-js "js/main.js"))

(defn header [title]
  (vector :head
          [:title title]
          [:link {:rel "icon"
                  :type "image/png"
                  :href "/favicon.png"}]
          (include-css "css/main.css")
          (include-bootstrap)))

(defn body [& content]
  (into [:body] content))

(defn om-page [ctx {:keys [title] :as opts}]
  (html5
   (header title)
   [:body
    [:p
     (-> ctx :request friend/current-authentication str)]
    [:div#app-container]
    (include-om)]))
