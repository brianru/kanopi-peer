(ns kanopi.util-dev
  (:require [com.stuartsierra.component :as component]
            [kanopi.aether.core :as aether]
            [kanopi.util.local-storage :as local-storage]
            [kanopi.controller.history.html5 :as history]
            [kanopi.controller.dispatch :as dispatch]
            [kanopi.model.ref-cursors :as ref-cursors]
            [kanopi.model.state :as state]
            ))

(def dev-config
  {:dimensions [:noun :verb]
   :aether-log true
   :mode :spa.unauthenticated/online})

(defn new-system
  ([]
   (new-system dev-config))
  ([config]
   (component/system-map
    :aether
    (aether/new-aether config)

    :history
    (component/using
     (history/new-html5-history config)
     {:aether :aether})

    :local-storage
    (local-storage/new-local-storage
     {:content-key (get config :local-storage-key "kanopi")})

    :app-state
    (component/using
     (state/new-app-state config)
     {:local-storage :local-storage})

    :dispatcher
    (component/using
     (dispatch/new-dispatcher config)
     {:aether    :aether
      :history   :history
      :app-state :app-state})
    
    )))

(def mock-search-results-app-state
  (atom {:search-results []}))

(defn shared-state [system]
  {:aether  (get-in system [:aether :aether])
   :history (get-in system [:history])
   :search-results
   (ref-cursors/mk-ref-cursor-fn mock-search-results-app-state :search-results)})

(defn app-state [system]
  (get-in system [:app-state :app-state]))
