(ns kanopi.view.core
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
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
         (println "here" (get-in props [:page]))
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
              (om/build thunk/container (get props :thunk)))
            ]
           )
         ]
        ]
       ))))

(defn mount-root! [app-state container ether history]
  (om/root root-component app-state {:target container, :shared {:ether (:ether ether)
                                                                 :history history}}))

(defrecord Om [config app-state ether history app-container]
  component/Lifecycle
  (start [this]
    ;; NOTE: purposely not checking if already mounted to support
    ;; figwheel re-mounting om on-jsload

    ;; TODO: do something with history component
    (let [container (. js/document (getElementById (:container-id config))) ]
      (info "mount om root" (:container-id config))
      (mount-root! (:app-state app-state) container ether history)
      (assoc this :app-container container)))

  (stop [this]
    (info "un-mount om root?")
    (assoc this :app-container nil)))

(defn new-om-root [config]
  (map->Om {:config config}))
