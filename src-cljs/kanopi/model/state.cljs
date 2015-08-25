(ns kanopi.model.state
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [goog.net.cookies :as cookies]
            [cemerick.url :as url]
            [cognitect.transit :as transit]
            ))

#_(defrecord PersistentAppState [config app-state])

(defn get-cookie [id]
  (-> (cookies/get id)
      (url/query->map)
      (get "init")
      (js/JSON.parse)
      (js->clj :keywordize-keys true)))

(defrecord EphemeralAppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [cookie (get-cookie "kanopi-init")
          atm (atom {:tempo {:pulse nil}
                     :user  (merge {:actions {}} (get cookie :user)) 
                     ;; I don't want to use the URI as a place to
                     ;; store state. All state is here.
                     :page  {}
                     :thunk {}

                     ;; local cache
                     ;; {<ent-id> <entity>}
                     :cache {}
                     })]
      (info "create ephemeral app state" @atm)
      (assoc this :app-state atm)))

  (stop [this]
    (info "destroy ephemeral app state")
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->EphemeralAppState {:config config}))
