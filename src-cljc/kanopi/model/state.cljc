(ns kanopi.model.state
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]
            [schema.core :as s]
            [kanopi.model.schema :as schema]
            [kanopi.util.core :as util]
            ))

(def ^:private default-config
  {:mode :spa.unauthenticated/online})

(def default-init-session
  (let [user-db-uuid (util/random-uuid)
        user-uuid (util/random-uuid)
        team-db-uuid (util/random-uuid)
        team {:db/id team-db-uuid
              :team/id user-uuid}]
    (hash-map
     :user {:ent-id user-db-uuid
            :username user-uuid
            :teams [team]
            :current-team team})))

(defrecord AppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [{:keys [mode]} (merge default-config config)
          init-session default-init-session
          atm (atom
               {:mode    mode
                :user    (get init-session :user)
                :page    nil
                :intent  nil
                :datum   (get init-session :datum)
                :literal (get init-session :literal)
                :most-viewed-datums []
                :most-edited-datums []
                :recent-datums      []
                :cache (get init-session :cache {})
                :search-results {}
                :error-messages []
                :log []
                }
               ; :validator (partial s/validate schema/AppState)
               )]
      (assoc this :app-state atm)))

  (stop [this]
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->AppState {:config config}))
