(ns kanopi.test-util
  (:require [com.stuartsierra.component :as component]
            [kanopi.main :refer (default-config)]
            [kanopi.system :refer (new-system)]))

(defn system-excl-web []
  (-> (new-system default-config)
      (dissoc :web-app :web-server)))

(defn system-excl-web-server []
  (-> (new-system default-config)
      (dissoc :web-server)))
