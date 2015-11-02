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

(defn login-button [submit-fn submittable]
  [:button.btn.btn-primary.btn-block
   {:on-click submit-fn
    :disabled (not submittable)}
   "Login"]
  )

(defn register-button [submit-fn submittable]
  [:button.btn.btn-warning.btn-block
   {:on-click submit-fn
    :disabled (not submittable)}
   "Register"])

(defn logout-button [submit-fn submittable]
  [:button.btn.btn-danger.btn-block
   {:on-click submit-fn}
   "Logout"])

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
            login-fn    #(login! owner (select-keys state [:username :password]))
            logout-fn   #(logout! owner)
            register-fn #(register! owner (select-keys state [:username :password]))
            default-submit-fn (case mode
                                ;; TODO: something more than
                                ;; (constantly nil). user must resolve
                                ;; ambiguity.)
                                :enter    (constantly nil)
                                :register register-fn
                                :login    login-fn
                                :logout   logout-fn
                                )
            submittable (not (some nil? [username password]))]
        (html
         [:div.container-fluid
          [:div.row
           [:div.col-md-offset-4.col-md-4

            [:div.panel.panel-default
             [:div.panel-heading
              [:h3.panel-title
               (case mode
                 :enter    "Welcome"
                 :register "Sign up"
                 :login    "Log in"
                 :logout   "Log out")]]
             [:div.panel-body
              (when (#{:register :login :enter} mode)
                [:div
                 (username-field owner :username default-submit-fn submittable)
                 (password-field owner :password default-submit-fn submittable)])

              (case mode
                :enter
                [:div
                 (login-button login-fn submittable)
                 (register-button register-fn submittable)]

                :login
                (login-button login-fn submittable)

                :register
                (register-button register-fn submittable)

                :logout
                (logout-button logout-fn submittable))
              ]
             [:div.panel-footer
              (case mode
                :enter
                [:div
                 [:span "Welcoem to Kanopi!"]]

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
