(ns kanopi.web.server
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [immutant.web :as web]
            [immutant.web.middleware :as middleware]
            [kanopi.web.routes :as routes]
            [kanopi.util.core :as util]
            ))

(defrecord WebServer
    [config web-app server-handle]
  component/Lifecycle
  (start [this]
    (if server-handle this
        (let [options       (select-keys config [:port :host])
              handler       (util/get-web-handler web-app)
              server-handle (if (:dev config)
                              ;; NOTE: not using web/run-dmc because
                              ;; it opens a web browser. sometimes dev
                              ;; mode is used in environments lacking
                              ;; a gui, such as docker
                              (web/run (middleware/wrap-development handler) options)
                              (web/run handler options))]
          (assoc this :server-handle server-handle))))
  (stop [this]
    (if-not server-handle this
            (do
              (web/stop server-handle)
              (assoc this :server-handle nil)))))

(defn new-web-server [config]
  (map->WebServer {:config config}))
