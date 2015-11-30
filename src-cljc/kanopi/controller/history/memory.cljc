(ns kanopi.controller.history.memory
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]
            [bidi.bidi :as bidi]
            [kanopi.controller.history :as history]
            [kanopi.model.routes :as routes]
            ))

(defrecord InMemoryHistory [config routes history]
  component/Lifecycle
  (start [this]
    (let [hist (atom [])]
      (info "start in-memory history")
      (assoc this :history hist, :routes routes/client-routes)))
  (stop [this]
    (swap! history empty)
    this)
  
  history/INavigator
  (get-route-for [this path]
    (let [path (if (coll? path) path [path])]
      (apply bidi/path-for routes path)))

  (navigate-to! [this path]
    (swap! history conj path)))

(defn new-mem-history [config]
  (map->InMemoryHistory {:config config}))
