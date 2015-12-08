(ns kanopi.util.local-storage
  (:require [com.stuartsierra.component :as component]
            [cognitect.transit :as transit]
            [kanopi.util.core :as util]
            #?(:clj [alandipert.enduro :as e])))

(defprotocol IPersistentStorage
  (get! [this default-value])
  (commit! [this v]))

#?(:cljs
   (defrecord LocalStorage [storage content-key]
     component/Lifecycle
     (start [this]
       (println "LOCAL STORAGE" content-key)
       (assoc this :storage (.-localStorage js/window)))
     (stop [this]
       (assoc this :storage nil))
     IPersistentStorage
     (get! [this default-value]
       (let [v (.getItem storage content-key)]
         (or (util/transit-read v) default-value)))
     (commit! [this v]
       (let [v' (util/transit-write v)]
         (.setItem storage content-key v'))))
   :clj
   (defrecord LocalStorage [config storage directory content-key]
     component/Lifecycle
     (start [this]
       ; NOTE: i'm not doing any checking to ensure directory exists
       ; or does not end with a forward slash. this will break at some
       ; point.
       (let [s (e/file-atom (get config :initial-value {})
                            (str directory "/storage.kanopi")
                            :pending-dir "target")]
         (assoc this :storage s)))
     (stop [this]
       ; NOTE: this deletes the file. Not sure if I want to do this.
       ; (e/release! storage)
       (assoc this :storage nil))
     
     IPersistentStorage
     (get! [this default-value]
       (get @storage content-key default-value))
     (commit! [this v]
       (e/swap! storage assoc content-key v))))

(defn new-local-storage [config]
  #?(:cljs (map->LocalStorage {:content-key (get config :content-key :kanopi)})
     :clj  (map->LocalStorage {:directory   (get config :directory "target")
                               :content-key (get config :content-key :kanopi)})))
