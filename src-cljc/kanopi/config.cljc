(ns kanopi.config
  "TODO: move all config logic here."
  (:require [schema.core :as s]))

(defn jvm-server-config []
  {})

(defn browser-client-config []
  {})

(defn jvm-client-config []
  {:local-storage
   {:initial-value {}}})
