(ns kanopi.system
  "TODO: I want a dynamic configuration component that can specify
  modal parameters. Generally this component would facilitate in
  switching other components' behavior between online and offline
  modes or demonstration and authenticated modes."
  (:require [com.stuartsierra.component :as component]
            [kanopi.view.core :as view]
            [kanopi.model.state.web :as state]
            [kanopi.controller.dispatch :as dispatch]
            [kanopi.controller.history.html5 :as history]
            [kanopi.aether.core :as aether]
            [kanopi.aether.spout :as aether-spout]
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
      :aether     :aether
      :history   :history})

    :local-storage
    (local-storage/new-local-storage (get config :local-storage-key "kanopi"))

    :app-state
    (component/using
     (state/new-app-state config)
     {:local-storage :local-storage})

    :aether
    (aether/new-aether config)

    :history
    (component/using
     (history/new-html5-history config)
     {:aether :aether})

    :dispatcher
    (component/using
     (dispatch/new-dispatcher config)
     {:aether    :aether
      :history   :history
      :app-state :app-state})

    ;; synchronous (useful responses)
     :request-spout
     (component/using
      (aether-spout/new-http-spout :verb :request {:xform (fn [msg] (get msg :noun))})
     {:aether    :aether
      :app-state :app-state})

    ;; asynchronous
    ;; (responses come back via another channel -- websockets?)
    ;;
    ;; :submit-spout
    ;; (component/using
    ;;  (aether-spout/new-http-spout :verb :submit config)
    ;;  {:aether :aether})
    
    
    )))
