(ns kanopi.util.browser
  (:require [om.core :as om]
            ))

(defn route-for [owner & args]
  (apply (om/get-shared owner [:history :route-for]) args))

(defn set-page! [owner path]
  (cond
   (string? path)
   ((om/get-shared owner [:history :set-page!]) path)

   (coll? path)
   (set-page! owner (apply route-for owner path))
   
   :default
   nil))

;; FIXME: this should not be here!
(defn input-element-for-entity-type [tp]
  (case tp
    :datum
    :input
    
    :literal/text
    :textarea
    
    ;; default
    :input))
