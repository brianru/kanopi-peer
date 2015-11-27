(ns kanopi.controller.history.html5
  "
  0) do not use URI as local state
     Only synchronize browser URI with app-state page data.

  1) navigate to a datum
     Wipe existing datum app-state.
     Try to initialize from cache.
     Regardless, request from server.
     => where are these tasks performed?
  
  TODO: kanopi.controller.router
  "
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.controller.history :as history]
            [kanopi.aether.core :as aether]
            [kanopi.model.message :as msg]
            [kanopi.model.routes :as routes]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]))

(defn- send-set-page-msg!
  "NOTE: this should match kanopi.model.messsage/navigate but cannot
  directly reference as it would create a circular dependency."
  [aether match]
  (->> (msg/navigate match)
       (aether/send! aether)))

(defrecord Html5History [config routes history aether]
  component/Lifecycle
  (start [this]
    (let [hist (pushy/pushy (partial send-set-page-msg! aether)
                            (partial bidi/match-route routes/client-routes))]
      (info "start html5 history")
      (pushy/start! hist)
      (assoc this :history hist :routes routes/client-routes)))

  (stop [this]
    (pushy/stop! history)
    (assoc this :history nil, :routes nil, :route-for (constantly nil)))
  
  history/INavigator
  (get-route-for [this path]
    (let [path (if (coll? path) path [path])]
      (apply bidi/path-for routes path)))

  (navigate-to! [this path]
    (pushy/set-token! history (get-route-for this path))))

(defn new-html5-history [config]
  (map->Html5History {:config config}))
