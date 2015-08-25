(ns kanopi.view.header
  (:require [om.core :as om]
            [sablono.core :refer-macros [html] :include-macros true]))

(defn header
  "
  Logout.
  Profile modal.
  Link to home.
  "
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "header")

    om/IRender
    (render [_]
      (let []
        (html
         [:div.header
          ])))
    )
  )
