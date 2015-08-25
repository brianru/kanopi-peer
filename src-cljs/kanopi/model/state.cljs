(ns kanopi.model.state
  (:require [quile.component :as component]))

#_(defrecord PersistentAppState [config app-state])

(defrecord EphemeralAppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [atm (atom {:tempo {:pulse nil}
                     :user  {:actions {}}
                     :data  {}})]
      (assoc this :app-state atm)))

  (stop [this]
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->EphemeralAppState {:config config}))
