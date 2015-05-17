(ns nebula.web.resources.auth
  (:require [liberator.core :refer [defresource]]
            [cemerick.friend :as friend]
            [nebula.web.resources.templates :as html]
            [ring.util.response :as r]
            [hiccup.page :refer (html5 include-js include-css)]))

(defn login-page [ctx]
  (let [{:keys [params]} (get ctx :request)]
    (html/page
     {:title "login"}
     [:div.container
      [:div.jumbotron
       {:style "top: 20vh; position: relative;"}
       [:div.row
        [:div.col-md-3]
        [:div.col-md-6
         [:h1 "nebula"]
         (when (= "Y" (get params :login-failed))
           [:div
            [:div.glyphicon.glyphicon-warning-sign.pull-left
             {:style "padding-right: 10px;"}
             ]
            [:p {:style "font-size: 16px; color: red;"}
             "Invalid username or password"]])
         [:form
          {:action "login", :method "post"}
          [:div.form-group.form-group-lg
           [:label {:for "username"}
            "Username"]
           [:input#username.form-control
            {:type "text"
             :required true
             :name "username"
             :value (get params :username)
             :placeholder "hannah"
             }]]
          [:div.form-group.form-group-lg
           [:label {:for "password"}
            "Password"]
           [:input#password.form-control
            {:type "password"
             :required true
             :name "password"
             :placeholder "rules"}]]
          [:button.btn.btn-primary.btn-lg.btn-block
           {:type "submit"} "Submit"]]]]]]
     )))

(defn logout! [ctx]
  (-> "/" r/redirect friend/logout*))

(defresource logout-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok logout!)

(defresource login-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  )
