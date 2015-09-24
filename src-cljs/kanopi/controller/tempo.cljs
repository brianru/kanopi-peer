(ns kanopi.controller.tempo
  "
  heart-rate-monitor
  progress-monitor
  mood-monitor
  goal-monitor
  
  TODO: zelkova."
  (:require [quile.component :as component]
            [kanopi.aether.core :as aether]
            [om.core :as om]
            ))


(defrecord TempoMonitor [config aether app-state message-stream kill-channel]
  component/Lifecycle
  (start [this]
    (if message-stream
      this
      (let []
        )))
  (stop [this]
    (if-not message-stream
      this
      (let []
        ))))

(defn new-tempo-monitor [config]
  (map->TempoMonitor {:config config}))
