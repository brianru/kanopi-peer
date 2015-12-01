(ns kanopi.model.state
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]
            [schema.core :as s]
            [kanopi.model.schema :as schema]
            ))

(def ^:private default-config
  {:mode :spa.unauthenticated/online})

(defrecord AppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [{:keys [mode]} (merge default-config config)
          atm (atom
               {:mode    mode
                :user    nil
                :page    nil
                :intent  nil
                :datum   nil
                :literal nil
                :most-viewed-datums []
                :most-edited-datums []
                :recent-datums      []
                :cache {}
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
