(ns kanopi.controller.ajax
  (:require [quile.component :as component]))

(defrecord AjaxSpout [config ether]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this))

(defn new-ajax-spout [config]
  (map->AjaxSpout {:config config}))
