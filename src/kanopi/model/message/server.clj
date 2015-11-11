(ns kanopi.model.message.server
  "Server-only utilities for parsing messages out of ring request maps"
  (:require [schema.core :as s]

            [taoensso.timbre :as timbre
             :refer (log trace debug info warn error fatal report)]
            [cemerick.friend :as friend]
            clojure.string
            [kanopi.model.schema :as schema]
            [kanopi.util.core :as util]))

;; TODO: refactor this to a schema transformation
;; https://github.com/Prismatic/schema/wiki/Writing-Custom-Transformations
;; http://blog.getprismatic.com/schema-0-2-0-back-with-clojurescript-data-coercion/
;; http://camdez.com/blog/2015/08/27/practical-data-coercion-with-prismatic-schema/
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- request->noun [ctx noun]
     {:post [(or (integer? %) (keyword? %) (instance? java.lang.Long %) (map? %))]}
     noun)
(defn- request->verb [ctx verb]
     {:post [(keyword? %)]}
     verb)
(defn- request->context [request-context message-context]
     {:post [(map? %)]}
     (let [creds (-> (friend/current-authentication (:request request-context))
                     :identity
                     ((util/get-auth-fn request-context))
                     )]
       (s/validate schema/Credentials creds)
       (assoc message-context :creds creds)))

(s/defn remote->local :- schema/Message
     "If for some reason the request is in some way logically incomplete,
     here's the place to indicate that."
     ([ctx]
      (let [body        (util/transit-read (get-in ctx [:request :body]))
            params      (get-in ctx [:request :params])
            ;; NOTE: keyword namespaces are stripped out by transit
            parsed-body (->> (merge body params)
                             (reduce (fn [acc [k v]]
                                       (cond
                                        (contains? #{:message/id :id} k)
                                        (assoc acc k v)

                                        (string? v)
                                        (if (clojure.string/blank? v)
                                          (assoc acc k {})
                                          (assoc acc k (util/try-read-string v))) 

                                        :default
                                        (assoc acc k v)))
                                     {}))]
        (hash-map
         :noun    (request->noun    ctx (:noun    parsed-body))
         :verb    (request->verb    ctx (:verb    parsed-body))
         :context (request->context ctx (:context parsed-body))))))
