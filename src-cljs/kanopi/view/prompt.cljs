(ns kanopi.view.prompt
  (:require [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros (html)]
            [om.core :as om]
   
   )
  )

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
      (let [current-prompt "Welcome to Kanopi!"]
        (html
         [:div.prompt.container-fluid
          [:div.row
           [:div.col-md-offset-4.col-md-4
            (render-prompt current-prompt)] ]
          ]))
      )))
