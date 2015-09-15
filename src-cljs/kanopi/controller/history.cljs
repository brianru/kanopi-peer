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
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.ether.core :as ether]
            [cljs.core.async :as async]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]))

(def default-routes ["/" {""         :home
                          "login"    :login
                          "logout"   :logout
                          "register" :register
                          "thunk/"   {[:id ""] :thunk}
                          "settings" :settings}])

(defn- send-set-page-msg! [ether match]
  (async/put! (get-in ether [:ether :publisher])
              {:noun match
               :verb :navigate
               :context nil}))

(defrecord Html5History [config routes route-for set-page! history ether]
  component/Lifecycle
  (start [this]
    (let [hist (pushy/pushy (partial send-set-page-msg! ether)
                            (partial bidi/match-route default-routes))]
      (info "start html5 history")
      (pushy/start! hist)
      (assoc this :history hist
             :routes default-routes
             :route-for (partial bidi/path-for default-routes)
             :set-page! (partial pushy/set-token! hist))))

  (stop [this]
    (pushy/stop! history)
    (assoc this :history nil, :routes nil, :route-for (constantly nil))))

(defn new-html5-history [config]
  (map->Html5History {:config config}))
