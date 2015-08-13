(ns kanopi.web.resources.auth
  (:require [liberator.core :refer [defresource]]
            [cemerick.friend :as friend]
            [kanopi.web.resources.templates :as html]
            [kanopi.web.auth :as auth]
            [kanopi.util.core :as util]
            [ring.util.response :as r]
            [hiccup.page :refer (html5 include-js include-css)]))

(defn login-page
  "TODO: Om Module.
  TODO: disable buttons until required fields are entered
  "
  [ctx]
  (let [{:keys [params]} (get ctx :request)]
    (html5
     (html/header "login")
     [:body
      [:div.container
       [:div.jumbotron
        {:style "top: 20vh; position: relative;"}
        [:div.row
         [:div.col-md-3
          ]
         [:div.col-md-6
          [:h1 "kanopi"]
          (when (= "Y" (get params :login_failed))
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
              :placeholder "hannah"}]]
           [:button.btn.btn-primary.btn-lg.btn-block
            {:type "submit"
             :value "login"}
            "Login"]
           ]]]]]
      ])))

(defn logout! [ctx]
  (-> "/" r/redirect friend/logout*))

(defn registration-page [ctx]
  (let [{:keys [params]} (get ctx :request)]
    (html5
     (html/header "register")
     [:body
      [:div.container
       [:div.jumbotron
        {:style "top: 20vh; position: relative;"}
        [:div.row
         [:div.col-md-3]
         [:div.col-md-6
          [:h1 "kanopi"]
          [:form
           {:action "register", :method "post"}
           [:div.form-group.form-group-lg
            [:label {:for "username"}
             "Username"]
            [:input#username.form-control
             {:type "text"
              :required true
              :name "username"
              :placeholder "bjr"
              }]
            ]
           [:div.form-group.form-group-lg
            [:label {:for "password"}
             "Password"]
            [:input#password.form-control
             {:type "password"
              :required true
              :name "password"
              :placeholder "rubinton"}]]
           [:button.btn.btn-success.btn-lg.btn-block
            {:type "submit", :value "register"}
            "Register"]
           ]]]]]])))

(defn register!
  "TODO: if success, redirect to internal page
  TODO: if failure, redirect to same page with fail msg in route params"
  [ctx]
  (let [{:keys [username password]} (get-in ctx [:request :params])
        authenticator (util/get-authenticator ctx)
        res @(auth/register! authenticator username password)]
    {::result res}))

(defn success?
  ""
  [ctx]
  (let [loc (if (not-empty (get-in ctx [::result :tx-data]))
              "/?welcome=true"
              "/register?fail=true")]
    {:location loc}))

(defresource logout-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok logout!)

(defresource login-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok login-page)

(defresource registration-resource
  :allowed-methods [:get :post]
  :available-media-types ["text/html"]
  :handle-ok registration-page
  :post! register!
  :post-redirect? success?)
