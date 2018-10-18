(ns kanopi.controller.handlers
  "NOTE: should really be kanopi.controller.handlers.request
  but there are currently no response handlers in the server."
  (:require [kanopi.model.data :as data]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.util.core :as util]
            [taoensso.timbre :as timbre
             :refer (log trace debug info warn error fatal report)]))

;; TODO: refactor to use session service
;; this is really initialize the client's session
#_(defmethod request-handler :spa.state/initialize
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
                  (success-verb (:verb message))
                  (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :user/change-password
    [request-context {:keys [noun verb] :as message}]
    (let [{:keys [current-password new-password confirm-new-password]} noun
                                        ; NOTE: must get username from creds. cannot trust anything
                                        ; user sends in message. must come from session.
          {:keys [username]} (get-in message [:context :creds])
          data (authenticator/change-password!
                (util/get-authenticator request-context)
                username current-password new-password confirm-new-password)]
      (hash-map
       :noun data
       :verb (if data
               (success-verb verb)
               (failure-verb verb))
       :context {})))

#_(defmethod request-handler :datum/create
    [request-context message]
    (let [data-svc (util/get-data-service request-context)
          creds    (get-in message [:context :creds])
          dtm-id   (data/init-datum data-svc creds)
          data     (data/user-datum data-svc creds dtm-id)]
      (hash-map
       :noun data
       :verb (if (get-in data [:datum :db/id])
               (success-verb (:verb message))
               (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :datum/get
    [request-context message]
    (let [data-svc (util/get-data-service request-context)
          creds    (get-in message [:context :creds])
          data     (data/user-datum data-svc creds (get message :noun))]
      (hash-map
       :noun    (or data {})
       :verb    (if (not-empty (get-in data [:datum]))
                  (success-verb (:verb message))
                  (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :datum.label/update
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
               (success-verb (:verb message))
               (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :datum.fact/add
    [request-context message]
    (let [data-svc  (util/get-data-service request-context)
          creds     (get-in message [:context :creds])
          datum-id  (get-in message [:noun :datum-id])
          fact      (get-in message [:noun :fact])
          result    (data/add-fact data-svc creds datum-id fact)
          datum'    (data/get-datum data-svc creds datum-id)]
      (hash-map
       :noun {:datum datum'
              :new-entites []}
       :verb (if result
               (success-verb (:verb message))
               (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :datum.fact/update
    [request-context message]
    (let [data-svc (util/get-data-service request-context)
          creds    (get-in message [:context :creds])
          datm-id  (get-in message [:noun :datum-id])
          fact     (get-in message [:noun :fact])
          result   (data/update-fact data-svc creds fact)
          datum'   (data/get-datum data-svc creds datm-id)
          ]
      (hash-map
       :noun {:datum datum'
              :new-entities []}
       :verb (if result
               (success-verb (:verb message))
               (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :literal/get
    [request-context message]
    (let [data-svc (util/get-data-service request-context)
          creds    (get-in message [:context :creds])
          data     (data/user-literal data-svc creds (get message :noun))]
      (hash-map
       :noun (or data {})
       :verb (if (not-empty (get data :literal))
               (success-verb (:verb message))
               (failure-verb (:verb message)))
       :context {})))

#_(defmethod request-handler :literal/update
    [request-context message]
    (let [data-svc (util/get-data-service request-context)
          creds    (get-in message [:context :creds])
          data (data/update-literal data-svc creds (get-in message [:noun :literal-id])
                                    (get-in message [:noun :new-type])
                                    (get-in message [:noun :new-value]))]
      (hash-map
       :noun nil
       :verb (if (not-empty data)
               (success-verb (:verb message))
               (failure-verb (:verb message)))
       :context {})))
