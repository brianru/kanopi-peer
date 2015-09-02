(ns kanopi.core
  (:require [quile.component :as component]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.system :as sys]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(enable-console-print!)

;; TODO: split up config by category or intended recipient component
(def dev-config
  {:container-id "app-container"
   :dimensions [:noun :verb]
   :ref-cursors [:search-results]})

(defonce system
  (component/start (sys/new-system dev-config)))

(defn reload-om []
  (component/start (get system :om)))
