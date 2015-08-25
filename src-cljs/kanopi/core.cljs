(ns kanopi.core
  (:require [quile.component :as component]
            [kanopi.system :as sys]
            [om.core :as om :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(def dev-config
  {:container-id "app-container"})

(defonce system
  (component/start (sys/new-system dev-config)))

(defn reload-om []
  (component/start (get system :om)))
