(ns kanopi.controller.history
  (:require [quile.component :as component]
            [kanopi.ether.core :as ether]))

(defprotocol IBrowserRouter
  (do-something [this foo]))

#_(defrecord HtmlHistory [config ether])

(defrecord Html5History [config ether]
  component/Lifecycle
  (start [this]
    this)

  (stop [this]
    this))

(defn new-html5-hsitory [config]
  (map->Html5History {:config config}))
