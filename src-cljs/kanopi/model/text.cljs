(ns kanopi.model.text
  (:require [kanopi.model.schema :as schema]))

(defn entity-value-label [ent]
  (case (schema/describe-entity ent)
    :datum
    "label"
    :literal
    "value"
    ;; default
    "label"))
