(ns kanopi.util-dev
  (:require [quile.component :as component]
            [kanopi.aether.core :as aether]
            [kanopi.controller.history :as history]
            [kanopi.controller.dispatch :as dispatch]
            [kanopi.model.ref-cursors :as ref-cursors]
            [kanopi.model.state :as state]
            ))

(def dev-config
  {:dimensions [:noun :verb]
   :aether-log true
   :mode :authenticated})

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

    :app-state
    (state/new-dev-app-state config)

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
