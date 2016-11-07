(ns kanopi.system.client
  (:require [com.stuartsierra.component :as component]

            #?(:cljs [kanopi.view.core :as view])
            #?(:cljs [kanopi.model.state.web :as state]
               :clj  [kanopi.model.state     :as state])
            #?(:cljs [kanopi.controller.history.html5  :as history]
               :clj  [kanopi.controller.history.memory :as history])

            [kanopi.util.local-storage :as local-storage]

            [kanopi.controller.dispatch :as dispatch]
            [kanopi.aether.core :as aether]
            [kanopi.aether.spout :as aether-spout]
            ))

#?(:clj
   (defn client-library [config]
     (component/system-map

      :local-storage
      (local-storage/new-local-storage (get config :local-storage))

      :app-state
      (component/using
       (state/new-app-state (get config :app-state))
       {:local-storage :local-storage})

      :history
      (history/new-mem-history config)

      :dispatcher
      (component/using
       (dispatch/new-fn-dispatcher config)
       {:history   :history
        :app-state :app-state})

      ;; synchronous (useful responses)
      ; :request-spout
      ; (component/using
      ;  (aether-spout/new-http-spout :verb :request {:xform (fn [msg] (get msg :noun))})
      ;  {:aether    :aether
      ;   :app-state :app-state})

      )))

#?(:cljs
   (defn web-app [config]
     (component/system-map

      :om
      (component/using
       (view/new-om-root config)
       {:app-state :app-state
        :aether     :aether
        :history   :history})

      :local-storage
      (local-storage/new-local-storage
       {:content-key (get config :local-storage-key "kanopi")})

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

(defn new-system
  ([] (new-system {}))
  ([config]
   #?(:clj  (client-library config)
      :cljs (web-app        config))))
