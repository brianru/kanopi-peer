(ns kanopi.view.pages.user
  "TODO: implement an ajax auth workflow.
  https://gist.github.com/ebaxt/11244031
  "
  (:require [sablono.core :refer-macros [html] :include-macros true]
            [om.core :as om]
            [kanopi.util.browser :as browser]
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
                         (println "success" resp))
              :error-handler (fn [resp]
                               (println "Error" resp))}))

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
        ])))
  )

(defn- register! [owner creds]
  (http/POST (browser/route-for owner :register)
             {:params creds
              :handler (fn [resp]
                         (println "success" resp))
              :error-handler (fn [resp]
                               (println "error" resp))}))

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

(defn- logout! []
  (http/GET "/logout"
            {:handler (fn [resp]
                        (println "success" resp))
             :error-handler (fn [resp]
                              (println "Error" resp))}))

(defn logout [props owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (logout!))
    om/IRender
    (render [_]
      (html
       [:div "Logout!"]))))
