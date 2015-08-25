(ns kanopi.view.pages.settings
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(defn settings [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "Settings")
    
    om/IRender
    (render [_]
      (let []
        (html
         [:div.settings])))))
