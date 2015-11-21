(ns kanopi.view.pages.teams
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(defn manage-teams [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "manage-teams")
    
    om/IRender
    (render [_]
      (html
       [:div.manage-teams]))))
