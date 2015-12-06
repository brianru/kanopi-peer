(ns kanopi.view.widgets.selector.list
  (:require [om.core :as om]
            [sablono.core :refer-macros (html)]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(defn vertical
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "vertical-list-selector")
    
    om/IRenderState
    (render-state [_ state]
      (let [
            ]
        (html
         [:div
          ])))))
