(ns kanopi.model.state
  (:require [quile.component :as component]))

(def app-state
  (atom {:tempo {:pulse nil}
         :user  {:actions {}}
         :data  {}}))

#_(defrecord PersistentAppState [config app-state])

(defrecord EphemeralAppState [config app-state]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this))

(defn new-app-state [config]
  (map->EphemeralAppState {:config config}))
