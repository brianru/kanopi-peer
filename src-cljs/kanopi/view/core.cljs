(ns kanopi.view.core
  (:require [quile.component :as component]
            [om.core :as om]))

(defn root
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "app-root")

    om/IRender
    (render [_]
      (html
       [:div
        [:h1 "Hi!"]]
       ))))

(defn mount-root! [app-state container ether]
  (om/root root-component app-state {:target container, :shared {:ether ether}}))

(defrecord Om [config app-state ether history app-container]
  component/Lifecycle
  (start [this]
    ;; NOTE: purposely not checking if already mounted to support
    ;; figwheel re-mounting om on-jsload

    ;; TODO: do something with history component
    (let [container (. js/document (getElementById (:container-id config))) ]
      (mount-root! (:atom app-state) container ether)
      (assoc this :app-container container)))

  (stop [this]
    (assoc this :app-container nil)))

(defn new-om-root [config]
  (map->Om {:config config}))
