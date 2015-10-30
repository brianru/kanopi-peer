(ns kanopi.controller.history
  "
  0) do not use URI as local state
     Only synchronize browser URI with app-state page data.

  1) navigate to a datum
     Wipe existing datum app-state.
     Try to initialize from cache.
     Regardless, request from server.
     => where are these tasks performed?
  
  "
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.aether.core :as aether]
            [cljs.core.async :as async]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]))

(def default-routes ["/" {
                          ;; SPA
                          ""         :home
                          "enter"    :enter
                          "login"    :login
                          "logout"   :logout
                          "register" :register
                          "settings" :settings
                          "datum/"   {[:id ""] :datum}
                          "literal/" {[:id ""] :literal}
                          
                          ;; Server
                          "api"      :api}])

(defn- send-set-page-msg! [aether match]
  (async/put! (get-in aether [:aether :publisher])
              {:noun match
               :verb :spa/navigate
               :context nil}))

(defprotocol INavigator
  (navigate-to! [this path])
  (get-route-for [this path]))

(defrecord Html5History [config routes route-for set-page! history aether]
  component/Lifecycle
  (start [this]
    (let [hist (pushy/pushy (partial send-set-page-msg! aether)
                            (partial bidi/match-route default-routes))]
      (info "start html5 history")
      (pushy/start! hist)
      (assoc this :history hist
             :routes default-routes
             :route-for (partial bidi/path-for default-routes)
             :set-page! (partial pushy/set-token! hist))))

  (stop [this]
    (pushy/stop! history)
    (assoc this :history nil, :routes nil, :route-for (constantly nil)))
  
  INavigator
  (get-route-for [this path]
    (let [path (if (coll? path) path [path])]
      (apply route-for path)))

  (navigate-to! [this path]
    ((get this :set-page!) (get-route-for this path))))

(defn new-html5-history [config]
  (map->Html5History {:config config}))
