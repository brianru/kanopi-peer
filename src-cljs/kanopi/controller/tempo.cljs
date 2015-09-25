(ns kanopi.controller.tempo
  "
  heart-rate-monitor
  progress-monitor
  mood-monitor
  goal-monitor
  
  TODO: zelkova.
  "
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [cljs.core.async :as async]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [quile.component :as component]
            [kanopi.aether.core :as aether]
            [om.core :as om]
            [jamesmacaulay.zelkova.signal :as signal]
            ))

(defrecord TempoMonitor [config aether app-state message-stream kill-channel]
  component/Lifecycle
  (start [this]
    (if message-stream
      this
      (let [kill-ch    (async/chan 1)
            msg-stream (aether/replicate! aether)]
        (info "start tempo monitor")
        ;; TODO: msg count signal
        (assoc this :message-stream msg-stream :kill-channel kill-ch))))
  (stop [this]
    (if-not message-stream
      this
      (let []
        (info "stop tempo monitor")
        ))))

(defn new-tempo-monitor [config]
  (map->TempoMonitor {:config config}))
