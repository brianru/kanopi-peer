(ns kanopi.view.pages.settings
  "Change username, password, contact information.
  Delete account.
  Payment stuff.
  Export all data."
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]

            [kanopi.view.widgets.panel :as panel]
            [kanopi.view.widgets.selector.list :as list]
            [kanopi.view.widgets.input-field :as input-field]
            ))

(defn change-password-template []
  (panel/default "Change password"
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
      "Submit"]]))

(defn delete-account-template []
  (panel/default "Delete account"
    [:button.btn.btn-danger
     {:type "submit"}
     "Delete your account"]))

(defn email-template []
  (panel/default "Emails"
    [:div]))

(def user-settings-items
  [{:ident :settings/account
    :label "Account settings"}
   {:ident :settings/emails
    :label "Emails"}
   ])

; TODO: hook settings pane into html5 history and routing
(defn settings [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "Settings")

    om/IInitState
    (init-state [_]
      {:settings-items  user-settings-items
       :current-item-id (-> user-settings-items first :ident)})
    
    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.settings.container
          [:div.row
           [:div.col-md-3.settings-pane-selector
            (list/vertical-menu "Personal settings"
                                (get state :current-item-id)
                                (get state :settings-items)
                                (partial om/set-state! owner :current-item-id))]

           [:div.col-md-9.settings-panel
            (case (get state :current-item-id)
              :settings/account
              [:div.selected-settings-pane
               (change-password-template)
               (delete-account-template)]

              :settings/emails
              [:div.selected-settings-pane
               (email-template)
               ]
              
              ;default
              nil)

            ]]])))))
