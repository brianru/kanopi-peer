(ns kanopi.system
  "TODO: I want a dynamic configuration component that can specify
  modal parameters. Generally this component would facilitate in
  switching other components' behavior between online and offline
  modes or demonstration and authenticated modes."
  (:require [quile.component :as component]
            [kanopi.view.core :as view]
            [kanopi.model.state :as state]
            [kanopi.controller.ajax :as ajax]
            [kanopi.controller.dispatch :as dispatch]
            [kanopi.controller.history :as history]
            [kanopi.ether.core :as ether]
            [kanopi.util.local-storage :as local-storage]
            ))

(defn new-system
  ([] (new-system {}))
  ([config]
   (component/system-map
    
    :om
    (component/using
     (view/new-om-root config)
     {:app-state :app-state
      :ether     :ether
      :history   :history})

    :local-storage
    (local-storage/new-local-storage (get config :local-storage-key "kanopi"))

    :app-state
    (component/using
     (state/new-app-state config)
     {:local-storage :local-storage})

    :ether
    (ether/new-ether config)

    :history
    (component/using
     (history/new-html5-history config)
     {:ether :ether})

    :dispatcher
    (component/using
     (dispatch/new-dispatcher config)
     {:ether :ether
      :app-state :app-state})

    :transporter
    (component/using
     (ajax/new-ajax-spout config)
     {:ether :ether})
    
    
    )))
