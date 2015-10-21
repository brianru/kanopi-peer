(ns kanopi.dev.util
  (:require [quile.component :as component]
            [kanopi.aether.core :as aether]
            ))

(def aether-config
  {:dimensions [:noun :verb]})

(defn new-system
  ([]
   (new-system aether-config))
  ([config]
   (component/system-map
    :aether
    (aether/new-aether config))))
