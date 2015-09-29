(ns kanopi.web.handlers
  (:require [kanopi.web.message :as msg]
            [kanopi.data :as data]
            [kanopi.util.core :as util]
            [clojure.pprint :refer (pprint)]
            ))

(defmulti request-handler
  (fn [request-context message]
    (println "Request-handler")
    (pprint message)
    (get message :verb))
  :default
  :echo)

(defmethod request-handler :echo
  [request-context message]
  (identity message))

(defmethod request-handler :get-thunk
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds (get-in message [:context :creds])
        data  (data/user-thunk data-svc creds (get message :noun))]
    (hash-map
     :noun    data
     :verb    (if (not-empty (get-in data [:thunk]))
                :get-thunk-success
                :get-thunk-failure)
     :context {})))

(defmethod request-handler :update-thunk-label
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds (friend/current-authentication (get request-context :request))
        data  (data/update-thunk-label data-svc creds
                                       (get-in message [:noun :existing-entity])
                                       (get-in message [:noun :new-label]))]
    (hash-map
     :noun data
     :verb (if data
             :update-thunk-label-success
             :update-thunk-label-failure)
     :context {})))
