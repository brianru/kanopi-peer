(ns kanopi.model.state
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [goog.net.cookies :as cookies]
            [cemerick.url :as url]
            [cognitect.transit :as transit]
            [kanopi.model.intro-data :refer (intro-data)]
            [kanopi.util.core :as util]
            [kanopi.util.local-storage :as local-storage]
            ))

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

;; FIXME: local storage must be stored per user
;; FIXME: must wipe out localstorage when user logs out
;; FIXME: this is accomplished simply by ensuring app-state is
;; properly updated when user logs in and out.
(defrecord LocalStorageAppState [config local-storage app-state]
  component/Lifecycle
  (start [this]
    (let [cookie (get-and-remove-cookie "kanopi-init")
          stored-app-state (local-storage/get! local-storage)
          atm (atom
               (util/deep-merge
                {:tempo {:pulse nil}
                 :user  (merge {:actions {}} (get cookie :user)) 
                 ;; I don't want to use the URI as a place to
                 ;; store state. All state is here.
                 :page  {}
                 :thunk {:context-thunks []
                         :thunk {}
                         :similar-thunks []
                         }
                 ;; TODO: this map grows too fast.
                 ;; implement a map that only stores the last n
                 ;; entries, everything else gets dropped off the
                 ;; back
                 :search-results {"foo" [[0.75 "food"] [0.42 "baffoon"]]}

                 ;; local cache
                 ;; {<ent-id> <entity>}
                 :cache (merge {} intro-data)
                 }
                stored-app-state
                ))]
      (info "create ephemeral app state" @atm)
      (assoc this :app-state atm)))

  (stop [this]
    (info "save app state to local storage")
    (local-storage/commit! local-storage @app-state)
    (info "destroy ephemeral app state")
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->LocalStorageAppState {:config config}))
