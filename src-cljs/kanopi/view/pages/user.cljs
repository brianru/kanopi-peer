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
  [:input
   {:type        "text"
    :placeholder "username"
    :value       (om/get-state owner username-key)
    :on-change   #(om/set-state! owner username-key (.. % -target -value))
    }])

(defn- password-field [owner password-key]
  [:input
   {:type        "password"
    :placeholder "password"
    :value       (om/get-state owner password-key)
    :on-change   #(om/set-state! owner password-key (.. % -target -value))
    }])

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

(defn login [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:username nil, :password nil})

    om/IRenderState
    (render-state [_ state]
      (html
       [:div
        (username-field owner :username)
        (password-field owner :password)
        [:button.btn.btn-primary
         {:on-click #(login! owner (select-keys state [:username :password]))}
         "Login"]
        [:button.btn.btn-warning
         {:on-click #(register! owner (select-keys state [:username :password]))}
         "Register"]
        ])))
  )

(defn register [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:username nil
       :password nil})

    om/IRenderState
    (render-state [_ state]
      (html
       [:div
        (username-field owner :username)
        (password-field owner :password)
        [:button.btn.btn-primary
         {:on-click #(register! owner (select-keys state [:username :password]))}
         "Register"]
        ]))))

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

(defn logout [props owner opts]
  (reify
    om/IRender
    (render [_]
      (html
       [:div "Logout!"]))))
