(ns kanopi.model.state.web
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [goog.net.cookies :as cookies]
            [cemerick.url :as url]
            [schema.core :as s]
            [kanopi.model.schema :as schema]
            [kanopi.util.core :as util]
            [kanopi.util.local-storage :as local-storage]
            ))

(defn get-cookie [id]
  (println "foo" (cookies/get id))
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

(defn get-init-session []
  (-> (js/document.getElementById "kanopi-init")
      (. -textContent)
      (->> (. js/JSON parse))
      (js->clj :keywordize-keys true)))

(defrecord LocalStorageAppState [config local-storage app-state]
  component/Lifecycle
  (start [this]
    (let [init-session     (get-init-session)
          stored-app-state {} ;(local-storage/get! local-storage {})
          atm (atom
               (util/deep-merge
                {
                 :mode :spa.unauthenticated/online
                 :user (get init-session :user {})
                 ;; I don't want to use the URI as a place to
                 ;; store state. All state is here.
                 :page (get init-session :page nil)
                 ;; used by header to do fancy modal stuff
                 :intent {:id :spa.unauthenticated/navigate}

                 ;; TODO: rename to current-datum
                 :datum (get init-session :datum
                             {:context-datums []
                              :similar-datums []
                              :datum          {}}) 
                 :literal {:literal {}
                           :context-datums []}

                 :most-viewed-datums []
                 :most-edited-datums []
                 :recent-datums      []

                 ;; local cache
                 ;; {<ent-id> <entity>}
                 :cache (get init-session :cache {})

                 ;; TODO: this map grows too fast.
                 ;; implement a map that only stores the last n
                 ;; entries, everything else gets dropped off the
                 ;; back
                 :search-results {"foo" [[0.75 "food"] [0.42 "baffoon"]]}
                 :error-messages []
                 :log []
                 }
                ; stored-app-state
                ))]
      #_(add-watch atm :validator (fn [_key _ref old-value new-value]
                                  (println "HERE")
                                  (when-let [errors (s/check schema/AppState new-value)]
                                    (println "App-state validation error!")
                                    (println errors)
                                    (println (keys new-value))
                                    )
                                  ; #_(try
                                  ;    (s/validate schema/AppState new-value)
                                  ;    (catch js/Object e
                                  ;      (println e)))
                                  ))
      (info "create ephemeral app state" @atm)
      (assoc this :app-state atm)))

  (stop [this]
    (info "save app state to local storage")
    #_(local-storage/commit! local-storage (dissoc @app-state :error-messages :search-results))
    (info "destroy ephemeral app state")
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->LocalStorageAppState {:config config}))
