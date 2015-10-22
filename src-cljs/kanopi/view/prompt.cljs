(ns kanopi.view.prompt
  (:require [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros (html)]
            [om.core :as om]
   
   )
  )

(defn compute-prompt
  ""
  [{:keys [error-messages] :as app-state}]
  (cond
   (not-empty error-messages)
   (hash-map
    :message (-> error-messages last :verb name)
    :type :error)
   ))

(defn render-prompt [p]
  [:a {}
   [:span (str p)]])

(defn prompt
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "prompt")
    
    om/IRender
    (render [_]
      (let [{tp :type, msg :message} (compute-prompt props)]
        (html
         [:div.prompt.container-fluid
          [:div.row
           [:div.col-md-offset-4.col-md-4
            (render-prompt msg)] ]
          ]))
      )))
