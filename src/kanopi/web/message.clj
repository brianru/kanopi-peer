(ns kanopi.web.message
  "TODO: be very careful about types here. Use lots of pre and post contracts.
  "
  (:require [kanopi.util.core :as util]
            [clojure.string]
            [cemerick.friend :as friend]))

(defn build-noun [ctx noun]
  noun)

(defn build-verb [ctx verb]
  {:post [(keyword? %)]}
  verb)

(defn build-context
  [request-context message-context]
  {:post [(map? %)]}
  (let []
    (assoc message-context
           :creds (friend/current-authentication (:request request-context)))))

(defn request-context->action-message
  "If for some reason the request is in some way logically incomplete,
  here's the place to indicate that."
  ([ctx]
   (let [body   (util/transit-read (get-in ctx [:request :body]))
         params (get-in ctx [:request :params])
         parsed-body (->> (merge body params)
                          (reduce (fn [acc [k v]]
                                    (if (clojure.string/blank? v)
                                      (assoc acc k {})
                                      (assoc acc k (read-string v))))
                                  {}))]
     (println "parsed-body" parsed-body)
     (hash-map
      :noun    (build-noun ctx (:noun parsed-body))
      :verb    (build-noun ctx (:verb parsed-body))
      :context (build-context ctx (:context parsed-body))))))
