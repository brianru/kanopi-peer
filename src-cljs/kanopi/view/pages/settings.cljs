(ns kanopi.view.pages.settings
  "Change username, password, contact information.
  Delete account.
  Payment stuff.
  Export all data."
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]

            [kanopi.view.widgets.selector.list :as list]
            [kanopi.view.widgets.input-field :as input-field]
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
         [:div.settings.container-fluid
          [:div.row
           [:div.col-md-3.settings-pane-selector
            [:div.panel.panel-default
             [:div.panel-heading
              [:h3.panel-title ""]]
             [:div.panel-body
              (om/build list/vertical props {:state {}})
              ]
             ]]

           [:div.col-md-9.settings-panel
            [:div.panel.panel-default
             [:div.panel-heading
              [:h2.panel-title "Change Password"]]
             [:div.panel-body
              [:div.edit-password
               ]]]]]])))))
