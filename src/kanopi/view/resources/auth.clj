(ns kanopi.view.resources.auth
  "BEWARE: this used to be an html/redirect-based auth flow. I then
  refactored it into a pure transit+json flow but I left the bones of
  the old flow because at some point I'll want both to work."
  (:require [liberator.core :refer [defresource]]
            [kanopi.util.liberator-transit]
            [cemerick.friend :as friend]
            [kanopi.view.resources.templates :as html]
            [kanopi.controller.auth :as auth]
            [kanopi.util.core :as util]
            [liberator.representation :as rep]
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
  [ctx]
  (let [params (get-in ctx [:request :params])
        body   (util/transit-read (get-in ctx [:request :body]))
        {:keys [username password]} (merge params body)
        authenticator (util/get-authenticator ctx)
        user-ent-id (auth/register! authenticator username password)
        ]
    {::result {:db/id user-ent-id}
     ;; NOTE: could this actually created an authenticated response?
     ;; I don't think that's happening b/c registration does not flow
     ;; through friend.
     ::identity (-> (auth/credentials authenticator username)
                    (select-keys [:ent-id :team :username])
                    ;; to make this match the friend
                    ;; current-authentication map
                    ((fn [x] (-> x
                                 (dissoc :team)
                                 (assoc :identity (:username x)
                                        :teams (->> x :team (map :db/id) (set)))
                                 ))))}))

(defn success?
  ""
  [ctx]
  (let [loc (if-let [id (get-in ctx [::result :db/id])]
              (str "/?welcome=true" "&id=" id)
              "/register?fail=true")]
    {:location loc}))

(defresource logout-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"
                          "application/transit+json"
                          "application/edn"
                          "application/json"]
  :handle-ok logout!)

(defresource login-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"
                          ]
  :handle-ok login-page)

(defresource ajax-login-resource
  :allowed-methods [:post]
  :available-media-types ["text/html"
                          "application/transit+json"
                          "application/edn"
                          "application/json"]
  :new? false
  :respond-with-entity? true
  ;; FIXME: media-type is always text/html? what is friend doing?
  :post-redirect? (fn [ctx]
                    (let [media-type (get-in ctx [:representation :media-type])]
                      (cond
                       (not= media-type "text/html")
                       false
                       
                       ;;default
                       (= media-type "text/html")
                       ;;false
                       ;; BEWARE: redirect causes session/cookie to be
                       ;; dropped somewhere in friend authentication
                       ;; middleware -> :session in response never
                       ;; makes it to ring session middleware
                       ;;
                        (if-let [current-auth (friend/current-authentication (:request ctx))]
                          {:location "/?welcome=true"}
                          {:location "/login?fail=true"})
                       )))

  :handle-ok (fn [ctx]
               (let [media-type (get-in ctx [:representation :media-type])]
                 (cond
                  (not= media-type "text/html")
                  (-> (friend/current-authentication (get-in ctx [:request]))
                      (rep/as-response ctx)
                      (rep/ring-response))
                  
                  (= media-type "text/html")
                  (-> "<h1>\"Success\"</h1>"
                      (rep/as-response ctx)
                      (rep/ring-response))))
               ))

(defresource ajax-logout-resource
  :allowed-methods [:post]
  :available-media-types ["text/html"
                          "application/transit+json"
                          "application/edn"
                          "application/json"]
  :new? false
  :respond-with-entity? true
  :post-redirect? (fn [ctx]
                    (let [media-type (get-in ctx [:representation :media-type])]
                      (cond
                       (not= media-type "text/html")
                       false
                       
                       ;;default
                       (= media-type "text/html")
                       {:location "/?logout=true"})))
  :handle-ok (fn [ctx]
               (-> (rep/ring-response {:logout-success true} ctx)
                   (friend/logout*))))

(defresource registration-resource
  :allowed-methods [:post]
  :available-media-types ["text/html"
                          "application/transit+json"
                          "application/edn"
                          "application/json"]
  :post! register!
  :new? false
  :respond-with-entity? true
  :post-redirect? (fn [ctx]
                    (let [media-type (get-in ctx [:representation :media-type])
                          ]
                      (cond
                       (not= media-type "text/html")
                       false

                       ;; default
                       (= media-type "text/html")
                       false
                       ;; BEWARE: redirect causes session/cookie to be
                       ;; dropped somewhere in friend authentication
                       ;; middleware -> :session in response never
                       ;; makes it to ring session middleware
                       ;;
                       ;;(if-let [user-id (get-in ctx [::identity :ent-id])]
                       ;;  {:location (str "/?welcome=true" "&id=" user-id)}
                       ;;  {:location "/register?fail=true"})

                       )))
  :handle-ok (fn [ctx]
               (let [media-type (get-in ctx [:representation :media-type])
                     user (get ctx ::identity {})]
                 (cond
                  (not= media-type "text/html")
                  ;; FIXME: this response does not match response from
                  ;; successful login. teams is destructed here.
                  (-> user
                      (rep/as-response ctx)
                      (friend/merge-authentication user)
                      (rep/ring-response))
                  
                  (= media-type "text/html")
                  (-> "<h1>\"Success!\"</h1>"
                      (rep/as-response ctx)
                      (friend/merge-authentication user)
                      (rep/ring-response))
                  )
                 )))
