(ns kanopi.view.pages.user
  (:require [sablono.core :refer-macros [html] :include-macros true]
            [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [ajax.core :as http])
  )

(defn- login! []
  nil)

(defn login [props owner opts]
  (reify
    om/IRender
    (render [_]
      (html [:div])))
  )

(defn- register! []
  nil)

(defn register [props owner opts]
  (reify
    om/IRender
    (render [_]
      (html [:div]))))

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
