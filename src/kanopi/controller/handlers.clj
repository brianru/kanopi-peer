(ns kanopi.controller.handlers
  "NOTE: should really be kanopi.controller.handlers.request
  but there are currently no response handlers in the server."
  (:require [kanopi.model.message :as msg]
            [kanopi.model.data :as data]
            [kanopi.util.core :as util]
            ))

(defmulti request-handler
  (fn [request-context message]
    (get message :verb))
  :default :echo)

(defmethod request-handler :echo
  [request-context message]
  (identity message))

;; TODO: refactor to use session service
;; this is really initialize the client's session
(defmethod request-handler :spa.state/initialize
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
                :spa.state.initialize/success
                :spa.state.initialize/failure)
     :context {})))

(defmethod request-handler :spa.navigate/search
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        data     (hash-map :search-results
                           (data/search-datums data-svc creds
                                               (get-in message [:noun :query-string])))
        ]
    (hash-map
     :noun    data
     ;; NOTE: really, I don't care if there are no search results.
     ;; that does not determine failure as in the other handlers. in
     ;; this case as long as the search ran let's return success.
     ;; I think it makes sense to indicate that by checking for the
     ;; search-results key in the response.
     :verb    (if (find data :search-results)
                :spa.navigate.search/success
                :spa.navigate.search/failure)
     :context {})))

(defmethod request-handler :datum/create
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        dtm-id   (data/init-datum data-svc creds)
        data     (data/user-datum data-svc creds dtm-id)]
    (hash-map
     :noun data
     :verb (if (get-in data [:datum :db/id])
             :datum.create/success
             :datum.create/failure)
     :context {})))

(defmethod request-handler :datum/get
  [request-context message]
  (let [data-svc (util/get-data-service request-context)
        creds    (get-in message [:context :creds])
        data     (data/user-datum data-svc creds (get message :noun))]
    (hash-map
     :noun    (or data {})
     :verb    (if (not-empty (get-in data [:datum]))
                :datum.get/success
                :datum.get/failure)
     :context {})))

(defmethod request-handler :datum.label/update
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
             :datum.label.update/success
             :datum.label.update/failure)
     :context {})))

(defmethod request-handler :datum.fact/add
  [request-context message]
  (let [data-svc  (util/get-data-service request-context)
        creds     (get-in message [:context :creds])
        datum-id  (get-in message [:noun :datum-id])
        fact      (get-in message [:noun :fact])
        result    (data/add-fact data-svc creds datum-id fact)
        data      (data/get-datum data-svc creds datum-id)]
    (hash-map
     :noun data
     :verb (if result
             :datum.fact.add/success
             :datum.fact.add/failure)
     :context {})))

(defmethod request-handler :datum.fact/update
  [request-context message]
  (let [data-svc  (util/get-data-service request-context)
        creds     (get-in message [:context :creds])
        datm-id   (get-in message [:noun :datum-id])
        fact      (get-in message [:noun :fact])
        result    (if (:db/id fact)
                    (data/update-fact data-svc creds fact)
                    (data/add-fact    data-svc creds datm-id fact)) 
        data      (data/get-datum data-svc creds datm-id)
        ]
    (hash-map
     :noun data
     :verb (if result
             :datum.fact.update/success
             :datum.fact.update/failure)
     :context {})))
