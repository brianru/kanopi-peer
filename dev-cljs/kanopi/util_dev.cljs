(ns kanopi.util-dev
  (:require [quile.component :as component]
            [kanopi.aether.core :as aether]
            [kanopi.controller.history :as history]
            [kanopi.model.ref-cursors :as ref-cursors]
            ))

(def aether-config
  {:dimensions [:noun :verb]})

(defn new-system
  ([]
   (new-system aether-config))
  ([config]
   (component/system-map
    :aether
    (aether/new-aether config)
    :history
    (component/using
     (history/new-html5-history config)
     {:aether :aether})
    
    
    )))

(def mock-search-results-app-state
  (atom {:search-results []}))

(defn shared-state [system]
  {:aether  (get-in system [:aether :aether])
   :history (get-in system [:history])
   :search-results
   (ref-cursors/mk-ref-cursor-fn mock-search-results-app-state :search-results)})
