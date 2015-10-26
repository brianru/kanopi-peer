(ns kanopi.model.message
  "TODO: be very careful about types here. Use lots of pre and post contracts.
  "
  (:require [kanopi.util.core :as util]
            [clojure.string]
            [cemerick.friend :as friend]
            [schema.core :as s]
            [kanopi.model.schema :as schema]
            ))

(defn- request->noun [ctx noun]
  {:post [(or (integer? %) (instance? java.lang.Long %) (map? %))]}
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

(defn remote->local
  "If for some reason the request is in some way logically incomplete,
  here's the place to indicate that."
  ([ctx]
   (let [body        (util/transit-read (get-in ctx [:request :body]))
         params      (get-in ctx [:request :params])
         parsed-body (->> (merge body params)
                          (reduce (fn [acc [k v]]
                                    (cond
                                     (string? v)
                                     (if (clojure.string/blank? v)
                                       (assoc acc k {})
                                       (assoc acc k (read-string v))) 
                                     
                                     :default
                                     (assoc acc k v)))
                                  {}))]
     (hash-map
      :noun    (request->noun    ctx (:noun    parsed-body))
      :verb    (request->verb    ctx (:verb    parsed-body))
      :context (request->context ctx (:context parsed-body))))))
