(ns nebula.test-util
  (:require [com.stuartsierra.component :as component]
            [nebula.main :refer (default-config)]
            [nebula.system :refer (new-system)]))

(defn system-excl-web []
  (-> (new-system default-config)
      (dissoc :web-app :web-server)))
