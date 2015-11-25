(ns kanopi.model.state
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]
            ))

(defrecord AppState [config app-state]
  component/Lifecycle
  (start [this]
    (let [{:keys [mode]} config
          atm (atom
               {:mode mode
                :user (if (= :spa.unauthenticated/online mode)
                        {}
                        {:ent-id 42
                         :identity "brian"
                         :username "brian"
                         :current-team {:db/id 27, :team/id "brian"}
                         :teams [{:db/id 27, :team/id "brian"}
                                 {:db/id 28, :team/id "hannah"}
                                 {:db/id 29, :team/id "cookie dough"}]
                         })
                :page {}
                :datum {:context-datums []
                        :similar-datums []
                        :datum          {}}
                :most-viewed-datums []
                :most-edited-datums []
                :recent-datums      []
                :cache {}
                :search-results {}
                :error-messages []})]
      (assoc this :app-state atm)))

  (stop [this]
    (assoc this :app-state nil)))

(defn new-app-state [config]
  (map->AppState {:config config}))
