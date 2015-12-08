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

(def user-settings-items
  [{:ident :settings/account
    :label "Account settings"}
   {:ident :settings/emails
    :label "Emails"}
   ])

(defn settings [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "Settings")

    om/IInitState
    (init-state [_]
      {:settings-items user-settings-items})
    
    om/IRenderState
    (render-state [_ state]
      (let [current :settings/account]
        (html
         [:div.settings.container
          [:div.row
           [:div.col-md-3.settings-pane-selector
            (list/vertical-menu "Personal settings"
                                current
                                (get state :settings-items)
                                (fn [selected-item]
                                  (info "implement this" selected-item)
                                  ))]

           [:div.col-md-9.settings-panel
            (change-password-template)
            (delete-account-template)
            
            ]]])))))
