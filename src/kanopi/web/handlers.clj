(ns kanopi.web.handlers
  (:require [kanopi.web.message :as msg]
            [kanopi.data :as data]))

(defmulti request-handler
  (fn [request-context message]
    (get message :verb))
  :default
  :echo)

(defmethod request-handler :echo
  [request-context message]
  message)

(defmethod request-handler :request
  [request-context message]
  (request-handler request-context (get message :noun)))
