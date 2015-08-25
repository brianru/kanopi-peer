(ns kanopi.view.header
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]))

(defn header
  "
  Logout.
  Settings.
  Home.
  "
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "header")

    om/IInitState
    (init-state [_]
      {:account-dropdown nil})

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.header.navbar.navbar-default.navbar-fixed-top
          [:div.container-fluid
           [:div.navbar-header
            [:a.navbar-brand
             {:on-click (fn [_]
                          (info "click header"))}
             "Kanopi"]]
           [:ul.nav.navbar-nav.navbar-right
            [:li.dropdown
             [:a.dropdown-toggle
              {:on-click (fn [_] (om/update-state! owner :account-dropdown not))
               :data-toggle "dropdown"
               :role "button"
               :aria-haspopup "true"
               :aria-expanded (if (get state :account-dropdown)
                                "true" "false")}
              "Dropdown" [:span.caret]]
             [:ul.dropdown-menu
              {:style {:display (if (get state :account-dropdown)
                                  "inherit")}
               }
              [:li [:a {} "Settings"]]
              [:li.divider {:role "separator"}]
              [:li [:a {} "Logout"]]]]]]
          ])))
    ))
