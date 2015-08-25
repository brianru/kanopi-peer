(ns kanopi.controller.ajax
  (:require [quile.component :as component]))

(defrecord AjaxSpout [config ether]
  component/Lifecycle
  (start [this]
    )
  (stop [this]
    ))

(defn new-ajax-spout [config]
  (map->AjaxSpout {:config config}))
