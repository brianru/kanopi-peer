(ns kanopi.view.header
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.util.browser :as browser]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.view.widgets.typeahead :as typeahead]
            [kanopi.model.schema :as schema]
            [kanopi.view.icons :as icons]
            [sablono.core :refer-macros [html] :include-macros true]))

;; TODO: quick-search in center of header
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

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.header.navbar.navbar-default.navbar-fixed-top
          [:div.container-fluid
           [:div.navbar-header
            [:a.navbar-brand
             {:href (browser/route-for owner :home)}
             "Kanopi"]]
           [:div.navbar-center
            (icons/search {})
            ;; TODO: this breaks when screen width <= 544px
            ;; Consider a clever interface, maybe only the searchglass
            ;; icon, when clicked, cover entire header with typeahead
            ;; search.
            [:span.search
             (om/build typeahead/typeahead props
                       {:init-state {:display-fn schema/display-entity
                                     :href-fn #(browser/route-for owner :thunk :id (:db/id %))
                                     :on-click (constantly nil)}})
             ]
            ]
           [:ul.nav.navbar-nav.navbar-right
            (if (get-in props [:user :identity])
              (om/build dropdown/dropdown props
                        {:init-state
                         {:toggle-label (get-in props [:user :username])
                          :menu-items [{:type  :link
                                        :href  (browser/route-for owner :settings)
                                        :label "Settings"}
                                       {:type  :divider}
                                       {:type  :link
                                        :href  (browser/route-for owner :logout)
                                        :label "Logout"}]
                          }})
              (->> (icons/log-in {})
                   (icons/link-to owner :login {:class "navbar-brand"}))
              )
            ]]
          ])))
    ))
