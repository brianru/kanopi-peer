(ns nebula.web.server
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [immutant.web :as web]
            [nebula.web.routes :as routes]
            [nebula.web.app :as web-app]))

(defrecord WebServer
    [config web-app server-handle]
  component/Lifecycle
  (start [this]
    (if server-handle this
        (let [options       (select-keys config [:port :host])
              handler       (web-app/get-handler web-app)
              server-handle (if (:dev config)
                              (web/run-dmc handler options)
                              (web/run handler options))]
          (assoc this :server-handle server-handle))))
  (stop [this]
    (if-not server-handle this
            (do
              (web/stop server-handle)
              (assoc this :server-handle nil)))))

(defn new-web-server [config]
  (map->WebServer {:config config}))
