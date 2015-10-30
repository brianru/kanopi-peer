(ns kanopi.view.header
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.util.browser :as browser]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.view.widgets.typeahead :as typeahead]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]
            [kanopi.view.icons :as icons]
            [sablono.core :refer-macros [html] :include-macros true]))

(defn center-search-field
  [props owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div.navbar-center
        (icons/search {})
        ;; FIXME: this breaks when screen width <= 544px
        ;; Consider a clever interface, maybe only the searchglass
        ;; icon, when clicked, cover entire header with typeahead
        ;; search.
        [:span.search
         (om/build typeahead/typeahead props
                   {:init-state {:display-fn schema/display-entity
                                 :href-fn #(browser/route-for owner :datum :id (:db/id %))
                                 :on-click (constantly nil)
                                 :tab-index 1}})]
        ]     )))
  )

(defn- team->menu-item [owner team]
  (hash-map :type     :link
            :on-click (fn [_]
                        (->> (msg/switch-team (:team/id team))
                             (msg/send! owner)))
            :label    (get team :team/id)
            ))

(defn left-team-dropdown
  [props owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div.navbar-header
        (if (get-in props [:user :current-team])
          (om/build dropdown/dropdown props
                    {:init-state
                     {:tab-index -1
                      }
                     :state
                     {
                      :toggle-label (get-in props [:user :current-team :team/id])
                      :menu-items (mapv (partial team->menu-item owner)
                                        (get-in props [:user :teams]))}
                     })
          [:a.navbar-brand
           {:href (browser/route-for owner :home)
            :tab-index -1}
           "Kanopi"]
          )]))))

(defn right-controls
  [props owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:ul.nav.navbar-nav.navbar-right
        #_(->> (icons/create {})
               (icons/on-click (constantly nil) {:class "navbar-brand"}))
        (->> (icons/insights {})
             (icons/on-click (constantly nil) {:class "navbar-brand"}))
        (if (get-in props [:user :identity])
          (om/build dropdown/dropdown props
                    {:init-state
                     {:toggle-label (get-in props [:user :current-team :team/id])
                      :tab-index -1
                      :menu-items [{:type  :link
                                    :href  (browser/route-for owner :settings)
                                    :label "Settings"}
                                   {:type  :divider}
                                   {:type  :link
                                    :href  (browser/route-for owner :logout)
                                    :label "Logout"}]
                      }})
          (->> (icons/log-in {})
               (icons/link-to owner :enter {:class "navbar-brand", :tab-index -1})))
        ]))
    ))

(defn header
  "A modal header. Contents will change based on the user's present
  intent. By default, we assume the user is trying to navigate. UI
  components will interpret user actions as user intentions, update
  the app state, and thus allow the header to help the user achieve
  her intention.

  TODO: figure out how we'll provide the header with any relevant
  state. The trick will be doing so in a relatively decoupled way so
  the header does not need to know about every possible mode, but
  instead categories of modes.
  "
  [props owner opts]
  (reify om/IDisplayName
    (display-name [_] "header")

    om/IRenderState
    (render-state [_ state]
      (html
       [:div.header.navbar.navbar-default.navbar-fixed-top
        (case (get-in props [:intent :id] :intent/navigate)
          :spa/navigate
          [:div.container-fluid
           (om/build left-team-dropdown props)
           (om/build center-search-field props)
           (om/build right-controls props)]
          )
        ]))))

