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
        ;; NOTE: do I need the icon?
        ;; (icons/search {})

        ;; FIXME: this breaks when screen width <= 544px
        ;; Consider a clever interface, maybe only the searchglass
        ;; icon, when clicked, cover entire header with typeahead
        ;; search.
        [:span.search
         (om/build typeahead/typeahead props
                   {:init-state
                    {:display-fn schema/display-entity
                     :placeholder "search"
                     :href-fn    (fn [result]
                                   (when-let [id (:db/id result)]
                                     (browser/route-for owner :datum :id id))) 
                     :on-click   (constantly nil)
                     :tab-index  1}})]
        ]))))

(defn- team->menu-item [owner team]
  (hash-map :type     :link
            :on-click (fn [_]
                        (->> (msg/switch-team (:team/id team))
                             (msg/send! owner)))
            :label    (get team :team/id)
            ))

(defn header-intent-dispatcher [props _]
  (get-in props [:intent :id] :spa.unauthenticated/navigate))

(defmulti left-team-dropdown
  header-intent-dispatcher)

(defmethod left-team-dropdown :spa.unauthenticated/navigate
  [props owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div.navbar-header
        [:a.navbar-brand
         {:href (browser/route-for owner :home)
          :tab-index -1}
         "Kanopi"]]))))

(defmethod left-team-dropdown :spa.authenticated/navigate
  [props owner]
  (reify
    om/IRender
    (render [_]
      (let [current-team (get-in props [:user :current-team])]
        (html
         [:div.navbar-header
          [:div.navbar-brand
           (om/build dropdown/dropdown props
                     {:init-state
                      {:tab-index -1
                       :toggle-type :split-button}
                      :state
                      {
                       :button-on-click (fn [_]
                                          (->> (msg/switch-team (:team/id current-team))
                                               (msg/send! owner)))
                       :toggle-label (:team/id current-team)
                       :menu-items (mapv (partial team->menu-item owner)
                                         (get-in props [:user :teams]))}
                      })]
          ]))
      )))

(defmulti right-controls
  header-intent-dispatcher)

(defmethod right-controls :spa.unauthenticated/navigate
  [props owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:ul.nav.navbar-nav.navbar-right
        (->> (icons/create {})
             (icons/on-click #(->> (msg/create-datum)
                                   (msg/send! owner))
                             {:class ["navbar-brand"]}))
        (->> (icons/goal {})
             (icons/on-click #(->> (msg/create-goal)
                                   (msg/send! owner))
                             {:class ["navbar-brand"]}))
        (->> (icons/insights {})
             (icons/on-click #(->> (msg/record-insight)
                                   (msg/send! owner))
                             {:class ["navbar-brand"]}))
        (->> (icons/log-in {})
               (icons/link-to owner :enter {:class "navbar-brand", :tab-index -1}))
        ]))))

(defmethod right-controls :spa.authenticated/navigate
  [props owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:ul.nav.navbar-nav.navbar-right
        (->> (icons/create {})
             (icons/on-click #(->> (msg/create-datum)
                                   (msg/send! owner))
                             {:class ["navbar-brand"]}))
        (->> (icons/goal {})
             (icons/on-click #(->> (msg/create-goal)
                                   (msg/send! owner))
                             {:class ["navbar-brand"]}))
        (->> (icons/insights {})
             (icons/on-click #(->> (msg/record-insight)
                                   (msg/send! owner))
                             {:class ["navbar-brand"]}))

        (om/build dropdown/dropdown props
                  {:init-state
                   {:toggle-label (get-in props [:user :identity])
                    :toggle-icon-fn icons/user
                    :classes ["navbar-brand"]
                    :caret? true
                    :tab-index -1
                    :menu-items [{:type  :link
                                  :href  (browser/route-for owner :settings)
                                  :label "Settings"}

                                 {:type  :divider}

                                 {:type  :link
                                  :href  ""
                                  :label "Feedback"}
                                 {:type  :link
                                  :href  ""
                                  :label "About"}
                                 {:type  :link
                                  :href  ""
                                  :label "Help"}

                                 {:type  :divider}

                                 {:type  :link
                                  :href  (browser/route-for owner :logout)
                                  :label "Logout"}]
                    }})
        ]))))

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
        [:div.container-fluid
         (om/build left-team-dropdown props)
         (om/build center-search-field props)
         (om/build right-controls props)
         ]
        ]))))

