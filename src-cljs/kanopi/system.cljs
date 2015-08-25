(ns kanopi.system
  (:require [quile.component :as component]
            [kanopi.view.core :as view]
            [kanopi.model.state :as state]
            [kanopi.controller.ajax :as ajax]
            [kanopi.ether.core :as ether]
            ))

(defn new-system
  ([] (new-system {}))
  ([config]
   (component/system-map
    
    :om
    (component/using
     (view/new-om-root config)
     :app-state :app-state
     :ether     :ether
     :history   :history)

    :app-state
    (state/new-app-state config)

    :ether
    (ether/new-ether config)

    :history
    (component/using
     (history/new-html5-history config)
     :ether :ether)

    :transporter
    (component/using
     (ajax/new-ajax-spout config)
     :ether :ether)
    
    
    )))
