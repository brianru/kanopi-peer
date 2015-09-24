(ns kanopi.view.pages.user
  "TODO: refactor to use aether and ajax spout for initiating GET/POST requests.
  "
  (:require [sablono.core :refer-macros [html] :include-macros true]
            [om.core :as om]
            [kanopi.util.browser :as browser]
            [kanopi.model.message :as msg]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]))

(defn- handle-key-down [owner submit-fn submittable evt]
  (case (.-key evt)
    "Enter"
    (when submittable
      (submit-fn))
    
    ;;default
    nil))

(defn- username-field [owner username-key submit-fn submittable]
  [:div.form-group
   [:input.form-control
    {:type        "text"
     :placeholder "username"
     :value       (om/get-state owner username-key)
     :on-change   #(om/set-state! owner username-key (.. % -target -value))
     :on-key-down (partial handle-key-down owner submit-fn submittable)
     }]])

(defn- password-field [owner password-key submit-fn submittable]
  [:div.form-group
   [:input.form-control
    {:type        "password"
     :placeholder "password"
     :value       (om/get-state owner password-key)
     :on-change   #(om/set-state! owner password-key (.. % -target -value))
     :on-key-down (partial handle-key-down owner submit-fn submittable)
     }]])

(defn- login! [owner creds]
  (->> creds
       (msg/login)
       (msg/send! owner)))

(defn- logout! [owner]
  (->> (msg/logout)
       (msg/send! owner)))

(defn- register! [owner creds]
  (->> creds
       (msg/register)
       (msg/send! owner)))

(defn authentication [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:username nil, :password nil})

    om/IRenderState
    (render-state [_ {:keys [username password] :as state}]
      (let [mode (get-in props [:page :handler])
            submit-fn (case mode
                        :login    #(login! owner (select-keys state [:username :password]))
                        :register #(register! owner (select-keys state [:username :password]))
                        :logout   #(logout! owner))
            submittable (not (some nil? [username password]))]
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
                 (username-field owner :username submit-fn submittable)
                 (password-field owner :password submit-fn submittable)])

              (case mode
                :login
                [:button.btn.btn-primary.btn-block
                 {:on-click submit-fn
                  :disabled (not submittable)}
                 "Login"]

                :register
                [:button.btn.btn-warning.btn-block
                 {:on-click submit-fn
                  :disabled (not submittable)}
                 "Register"]

                :logout
                [:button.btn.btn-danger.btn-block
                 {:on-click submit-fn}
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
         ))
      ))
  )
