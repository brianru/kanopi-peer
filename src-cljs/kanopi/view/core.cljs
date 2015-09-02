(ns kanopi.view.core
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.model.ref-cursors :as ref-cursors]
            [kanopi.model.message :as msg]
            [kanopi.view.header :as header]
            [kanopi.view.thunk :as thunk]
            [kanopi.view.pages.settings :as settings]
            [kanopi.view.pages.user :as user]
            [kanopi.ether.core :as ether]
            [kanopi.controller.handlers :as handlers]
            [kanopi.util.browser :as browser]
            [ajax.core :as http]
            [cljs.core.async :as async]
            [om.core :as om]))

(defn root-component
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "app-root")

    om/IRender
    (render [_]
      (html
       [:div
        [:div.header-container
         (om/build header/header props)]
        [:div.page-container
         (case (get-in props [:page :handler])
           :thunk
           (om/build thunk/container (get props :thunk))

           :settings
           (om/build settings/settings props)

           :login
           (om/build user/login props)

           :register
           (om/build user/register props)

           :logout
           (om/build user/logout props)

           ;; TODO: welcome thunk
           [:div.home-page
            (let [thunks (->> (get props :cache)
                              (vals))]
              [:ul
              (for [t thunks
                    :when (:thunk/label t)]
                [:li
                 [:a {:href (browser/route-for owner :thunk :id (:db/id t))}
                  [:span (:thunk/label t)]] ]
                )])
            ]
           )
         ]
        ]
       ))))

(defn- search-results-ref-cursor [app-state]
  (->> app-state
       (om/root-cursor)
       (:search-results)
       (om/ref-cursor)))

(defn mount-root! [app-state container ether history ref-cursors]
  (om/root root-component
           app-state
           {:target container
            :shared (merge
                     {:ether (:ether ether)
                      :history history}
                     (ref-cursors/mk-ref-cursor-map app-state ref-cursors))}))

(defrecord Om [config app-state ether history app-container]
  component/Lifecycle
  (start [this]
    ;; NOTE: purposely not checking if already mounted to support
    ;; figwheel re-mounting om on-jsload
    (let [container (. js/document (getElementById (:container-id config))) ]
      (info "mount om root" (:container-id config))
      (mount-root! (:app-state app-state) container ether history (:ref-cursors config))
      (assoc this :app-container container)))

  (stop [this]
    (info "un-mount om root?")
    (assoc this :app-container nil)))

(defn new-om-root [config]
  (map->Om {:config config}))
