(ns kanopi.controller.history
  "
  0) do not use URI as local state
     Only synchronize browser URI with app-state page data.

  1) navigate to a thunk
     Wipe existing thunk app-state.
     Try to initialize from cache.
     Regardless, request from server.
     => where are these tasks performed?
  
  "
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

(defn new-html5-history [config]
  (map->Html5History {:config config}))
