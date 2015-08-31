(ns kanopi.core
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.system :as sys]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(enable-console-print!)

(def dev-config
  {:container-id "app-container"
   :dimensions [:noun :verb]})

(defonce system
  (component/start (sys/new-system dev-config)))

(defn reload-om []
  (component/start (get system :om)))
