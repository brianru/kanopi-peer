(ns kanopi.util.local-storage
  (:require [com.stuartsierra.component :as component]
            [cognitect.transit :as transit]))

(defprotocol IPersistentStorage
  (get! [this default-value])
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
  (get! [this default-value]
    (let [v (.getItem storage k)]
      (or (transit/read reader v) default-value)))
  (commit! [this v]
    (let [v' (transit/write writer v)]
      (.setItem storage k v'))))

(defn new-local-storage [k]
  (LocalStorage. nil nil nil k))
