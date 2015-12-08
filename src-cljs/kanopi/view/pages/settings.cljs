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

(defn change-password-template []
  (let []
    [:div.panel.panel-default
     [:div.panel-heading
      [:h2.panel-title "Change password"]]
     [:div.panel-body
      [:form
       [:div.form-group
        [:label {:for "currentPassword"}
         "Old password"]
        [:input.form-control
         {:id "currentPassword"
          :type "password"}]]
       [:div.form-group
        [:label {:for "newPassword"}
         "New password"]
        [:input.form-control
         {:id "newPassword"
          :type "password"}]]
       [:div.form-group
        [:label {:for "confirmNewPassword"}
         "Confirm new password"]
        [:input.form-control
         {:id "confirmNewPassword"
          :type "password"}]]
       [:button.btn.btn-default
        {:type "submit"}
        "Submit"]]
      ]]))

(defn delete-account-template []
  (let []
    [:div.panel.panel-danger
     [:div.panel-heading
      [:h2.panel-title "Delete account"]]
     [:div.panel-body
      [:button.btn.btn-danger
       {:type "submit"}
       "Delete your account"]]]))

(defn settings [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "Settings")
    
    om/IRender
    (render [_]
      (let [current :settings/account]
        (html
         [:div.settings.container
          [:div.row
           [:div.col-md-3.settings-pane-selector
            [:nav.menu
             [:h3.menu-heading "Personal settings"]
             [:a.menu-item
              {:class [(when (= :settings/account current)
                         "selected")]}
              "Account settings"]
             [:a.menu-item
              {:class [(when (= :settings/emails current)
                         "selected")]}
              "Emails"]]
            ]

           [:div.col-md-9.settings-panel
            (change-password-template)
            (delete-account-template)
            
            ]]])))))
