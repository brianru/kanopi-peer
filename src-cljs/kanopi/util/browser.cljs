(ns kanopi.util.browser
  (:require [om.core :as om]))

(defn route-for [owner & args]
  (apply (om/get-shared owner [:history :route-for]) args))

(defn set-page! [owner path]
  {:pre [(string? path)]}
  ((om/get-shared owner [:history :set-page!]) path))

(defn input-element-for-entity-type [tp]
  (case tp
    :datum
    :input
    
    :literal/text
    :textarea
    
    ;; default
    :input))
