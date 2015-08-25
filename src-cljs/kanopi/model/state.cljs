(ns kanopi.model.state
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

#_(defrecord PersistentAppState [config app-state])

(defrecord EphemeralAppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [atm (atom {:tempo {:pulse nil}
                     :user  {:actions {}}
                     ;; I don't want to use the URI as a place to
                     ;; store state. All state is here.
                     :page  {}
                     :thunk {}

                     ;; local cache
                     ;; {<ent-id> <entity>}
                     :cache {}
                     })]
      (info "create ephemeral app state")
      (assoc this :app-state atm)))

  (stop [this]
    (info "destroy ephemeral app state")
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->EphemeralAppState {:config config}))
