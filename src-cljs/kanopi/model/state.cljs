(ns kanopi.model.state
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [goog.net.cookies :as cookies]
            [cemerick.url :as url]
            [cognitect.transit :as transit]
            [kanopi.model.intro-data :refer (intro-data)]
            ))

#_(defrecord PersistentAppState [config app-state])

(defn get-cookie [id]
  (-> (cookies/get id)
      (url/query->map)
      (get "init")
      (js/JSON.parse)
      (js->clj :keywordize-keys true)))

(defn delete-cookie! [id]
  (cookies/remove id))

(defn get-and-remove-cookie
  "Only for transferring initial state to client session, not for
  persisting data between page refreshes."
  [id]
  (let [c (get-cookie id)]
    (delete-cookie! id)
    c))

(defrecord EphemeralAppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [cookie (get-and-remove-cookie "kanopi-init")
          atm (atom {:tempo {:pulse nil}
                     :user  (merge {:actions {}} (get cookie :user)) 
                     ;; I don't want to use the URI as a place to
                     ;; store state. All state is here.
                     :page  {}
                     :thunk {:context-thunks (select-keys intro-data [-1006 -1008 -1016]) 
                             :thunk (get intro-data -1000)
                             :similar-thunks (select-keys intro-data [-1009 -1012 -1016])
                             }
                     :search-results {"foo" ["food" "baffoon"]}

                     ;; local cache
                     ;; {<ent-id> <entity>}
                     :cache (merge {} intro-data)
                     })]
      (info "create ephemeral app state" @atm)
      (assoc this :app-state atm)))

  (stop [this]
    (info "destroy ephemeral app state")
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->EphemeralAppState {:config config}))
