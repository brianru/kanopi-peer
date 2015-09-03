(ns kanopi.util.browser
  (:require [om.core :as om]))

(defn route-for [owner & args]
  (apply (om/get-shared owner [:history :route-for]) args))

(defn input-element-for-entity-type [tp]
  (case tp
    :thunk
    :input
    
    :literal/text
    :textarea
    
    ;; default
    :input))
