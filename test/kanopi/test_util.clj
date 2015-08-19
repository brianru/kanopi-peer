(ns kanopi.test-util
  (:require [com.stuartsierra.component :as component]
            [kanopi.main :refer (default-config)]
            [kanopi.system :refer (new-system)]))

(defonce ^:dynamic *system* nil)

(defn system-excl-web []
  (-> (new-system default-config)
      (dissoc :web-app :web-server)))

(defn system-excl-web-fixture [f]
  (with-bindings [*system* (component/start (system-excl-web))]
    (f)
    (component/stop *system*)))
