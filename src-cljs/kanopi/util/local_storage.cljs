(ns kanopi.util.local-storage
  (:require [quile.component :as component]
            [cognitect.transit :as transit]))

(defprotocol IPersistentStorage
  (get! [this])
  (commit! [this v]))

(defrecord LocalStorage [storage writer reader k]
  component/Lifecycle
  (start [this]
    (assoc this
           :storage (.-localStorage js/window)
           :writer  (transit/writer :json)
           :reader  (transit/reader :json)))
  (stop [this]
    (assoc this
           :storage nil
           :writer  nil
           :reader  nil))
  IPersistentStorage
  (get! [this]
    (let [v (.getItem storage k)]
      (transit/read reader v)))
  (commit! [this v]
    (let [v' (transit/write writer v)]
      (.setItem storage k v'))))

(defn new-local-storage [k]
  (LocalStorage. nil nil nil k))
