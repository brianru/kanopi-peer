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

(defmethod request-handler :initialize-client-state
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        data     (hash-map
                  :most-edited-datums (data/most-edited-datums data-svc creds)
                  :most-viewed-datums (data/most-viewed-datums data-svc creds)
                  :recent-datums      (data/recent-datums data-svc creds))]
    (hash-map
     :noun    data
     :verb    (if (->> data (vals) (apply concat) (not-empty))
                :initialize-client-state-success
                :initialize-client-state-failure)
     :context {})))

(defmethod request-handler :get-datum
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        data     (data/user-datum data-svc creds (get message :noun))]
    (hash-map
     :noun    data
     :verb    (if (not-empty (get-in data [:datum]))
                :get-datum-success
                :get-datum-failure)
     :context {})))

(defmethod request-handler :update-datum-label
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        data     (data/update-datum-label
                  data-svc creds
                  (get-in message [:noun :existing-entity])
                  (get-in message [:noun :new-label]))]
    (hash-map
     :noun data
     :verb (if data
             :update-datum-label-success
             :update-datum-label-failure)
     :context {})))

(defmethod request-handler :update-fact
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        ;;data     (data/update-fact
        ;;          data-svc creds
        ;;          (get message :noun))
        ]))
