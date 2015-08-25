(ns kanopi.web.resources.templates
  (:require [hiccup.page :refer (html5 include-js include-css)]
            [liberator.representation :as rep]
            [cheshire.core :as json]
            [kanopi.util.core :as util]))

(defn include-bootstrap []
  (include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"))

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

(defn om-page
  "TODO: set cookie with no expiration (expire at end of session)
  which contains user identity and recently modified thunks and config information"
  [ctx {:keys [title cookie] :as opts}]
  (let []
    (rep/ring-response
     {:cookies
      {"kanopi-init"
       {:value {:init (json/generate-string cookie)}}}

      :body
      (html5
       (header title)
       [:body
        [:div#app-container]
        (include-om)])
      })))
