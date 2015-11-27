(ns kanopi.util.browser
  (:require [om.core :as om]
            [kanopi.controller.history :as history]
            ))

(defn history [owner]
  (om/get-shared owner [:history]))

(defn route-for [owner & args]
  (history/get-route-for (history owner) args))

(defn set-page! [owner path]
  (cond
   (string? path)
   (history/navigate-to! (history owner) path)

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
