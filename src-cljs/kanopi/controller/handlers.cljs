(ns kanopi.controller.handlers
  "All app-state transformations are defined here.

  TODO: refactor to work with om cursors instead of atoms."
  (:require [om.core :as om]
            [kanopi.util.core :as util]
            [kanopi.aether.core :as aether]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]
            [kanopi.controller.history :as history]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(defn log-msg! [app-state msg]
  (om/transact! app-state :log
                (fn [log]
                  (if (< (count log) 100)
                    (conj log msg)
                    (vector msg)))))

(defmulti local-event-handler
  (fn [aether history app-state msg]
    (println "local-event-handler" msg)
    ;; This may not be idiomatic, but it's the simplest place to do
    ;; it.
    (log-msg! app-state msg)

    (get msg :verb))
  :default
  :log)

(defmethod local-event-handler :log
  [aether history app-state msg]
  (info msg)
  msg)
