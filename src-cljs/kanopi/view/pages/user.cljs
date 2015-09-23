(ns kanopi.view.pages.user
  "TODO: refactor to use aether and ajax spout for initiating GET/POST requests.
  "
  (:require [sablono.core :refer-macros [html] :include-macros true]
            [om.core :as om]
            [kanopi.util.browser :as browser]
            [kanopi.model.message :as msg]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [ajax.core :as http]))

(defn- username-field [owner username-key]
  [:div.form-group
   [:input.form-control
    {:type        "text"
     :placeholder "username"
     :value       (om/get-state owner username-key)
     :on-change   #(om/set-state! owner username-key (.. % -target -value))
     }]])

(defn- password-field [owner password-key]
  [:div.form-group
   [:input.form-control
    {:type        "password"
     :placeholder "password"
     :value       (om/get-state owner password-key)
     :on-change   #(om/set-state! owner password-key (.. % -target -value))
     }]])

(defn- login! [owner creds]
  (http/POST (browser/route-for owner :login)
             {:params creds
              :handler (fn [resp]
                         (->> resp
                              (msg/login-success)
                              (msg/send! owner)))
              :error-handler (fn [resp]
                               (->> resp
                                    (msg/login-failure)
                                    (msg/send! owner)))}))

(defn- logout! [owner]
  (http/GET "/logout"
            {:handler (fn [resp]
                        (->> resp
                             (msg/logout-success)
                             (msg/send! owner)))
             :error-handler (fn [resp]
                              (->> resp
                                   (msg/logout-failure)
                                   (msg/send! owner)))}))

(defn- register! [owner creds]
  (http/POST (browser/route-for owner :register)
             {:params creds
              :handler (fn [resp]
                         (->> resp
                              (msg/register-success)
                              (msg/send! owner)))
              :error-handler (fn [resp]
                               (->> resp
                                    (msg/register-failure)
                                    (msg/send! owner)))}))

(defn authentication [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:username nil, :password nil})

    om/IRenderState
    (render-state [_ {:keys [username password] :as state}]
      (let [mode (get-in props [:page :handler])]
        (html
         [:div.container-fluid
          [:div.row
           [:div.col-md-offset-4.col-md-4

            [:div.panel.panel-default
             [:div.panel-heading
              [:h3.panel-title
               (case mode
                 :register "Sign up"
                 :login    "Log in"
                 :logout   "Log out")]]
             [:div.panel-body

              (when (#{:register :login} mode)
                [:div
                 (username-field owner :username)
                 (password-field owner :password)])

              (case mode
                :login
                [:button.btn.btn-primary.btn-block
                 {:on-click #(login! owner (select-keys state [:username :password]))
                  :disabled (some nil? [username password])}
                 "Login"]

                :register
                [:button.btn.btn-warning.btn-block
                 {:on-click #(register! owner (select-keys state [:username :password]))
                  :disabled (some nil? [username password])}
                 "Register"]

                :logout
                [:button.btn.btn-danger.btn-block
                 {:on-click #(logout! owner)}
                 "Logout"]
                )


              ]
             [:div.panel-footer
              (case mode
                :register
                [:div
                 [:span "Have an account? "]
                 [:a {:href (browser/route-for owner :login)} "Log in!"] ]

                :login
                [:div
                 [:span "New to Kanopi? "]
                 [:a {:href (browser/route-for owner :register)} "Sign up!"]]

                :logout
                [:div
                 [:span "Are you sure you want to leave? "]
                 [:a {:href (browser/route-for owner :home)} "Home"]]
                )

              ]]
            ]]]
         )  )
      ))
  )
