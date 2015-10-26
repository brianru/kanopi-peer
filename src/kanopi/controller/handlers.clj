(ns kanopi.controller.handlers
  (:require [kanopi.model.message :as msg]
            [kanopi.model.data :as data]
            [kanopi.util.core :as util]
            [clojure.pprint :refer (pprint)]
            ))

(defmulti request-handler
  (fn [request-context message]
    (get message :verb))
  :default :echo)

(defmethod request-handler :echo
  [request-context message]
  (identity message))

(defmethod request-handler :initialize-client-state
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])

        most-edited-datums (data/most-edited-datums data-svc creds)
        most-viewed-datums (data/most-viewed-datums data-svc creds)
        recent-datums      (data/recent-datums data-svc creds)

        ;; NOTE: map then concat because i have not settled on the
        ;; shape of each collections' contents
        all-datum-ids (concat
                       (map first most-edited-datums)
                       (map first most-viewed-datums)
                       (map first recent-datums)) 
        ;; NOTE: cache is here to ensure the above datums show up in
        ;; search results -- right now search results are only local.
        ;; really that's wrong, searches should be executed on the
        ;; server. AH.
        ;; TODO: implement server-side search.
        ;;all-datums    (->> all-datum-ids
        ;;                   (map (partial data/get-datum data-svc creds))
        ;;                   (reduce (fn [acc datum]
        ;;                             (assoc acc (:db/id datum) datum))
        ;;                           {}))

        data     (hash-map
                  :most-edited-datums most-edited-datums
                  :most-viewed-datums most-viewed-datums
                  :recent-datums      recent-datums
                  ;;:cache              all-datums
                  )
        ]
    (hash-map
     :noun    data
     :verb    (if (-> all-datum-ids not-empty)
                :initialize-client-state-success
                :initialize-client-state-failure)
     :context {})))

(defmethod request-handler :search
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        data     (hash-map :search-results
                           (data/search-datums data-svc creds (get message :noun)))
        ]
    (hash-map
     :noun    data
     ;; NOTE: really, I don't care if there are no search results.
     ;; that does not determine failure as in the other handlers. in
     ;; this case as long as the search ran let's return success.
     ;; I think it makes sense to indicate that by checking for the
     ;; search-results key in the response.
     :verb    (if (find data :search-results)
                :search-success
                :search-failure)
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
  (let [data-svc  (util/get-data-service request-context)
        creds     (get-in message [:context :creds])
        datm-id   (get-in message [:noun :datum-id])
        fact      (get-in message [:noun :fact])
        _ (println "UPDATE_FACT REQUEST_HANDLER" fact)
        data      (if (:db/id fact)
                    (data/update-fact data-svc creds fact)
                    (data/add-fact    data-svc creds datm-id fact)) 
        ]
    (hash-map
     :noun data
     :verb (if data
             :update-fact-success
             :update-fact-failure)
     :context {})))
