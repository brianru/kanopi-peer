(ns nebula.system
  (:require [com.stuartsierra.component :as component]
            [nebula.storage.datomic :refer [database]]))

(defn new-system
  [config]
  (let [{:keys [port env]} config]
    (component/system-map
     :db (database "localhost" 4334))))
