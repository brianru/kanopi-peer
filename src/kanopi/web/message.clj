(ns kanopi.web.message
  "TODO: be very careful about types here. Use lots of pre and post contracts.
  "
  (:require [kanopi.util.core :as util]
            [clojure.string]
            [cemerick.friend :as friend]))

#_(defn build-noun [ctx]
    {:post [(when-let [eid (:ent-id %)]
              (integer? eid))]}
    (-> ctx
        (get-in [:request :params])
        (select-keys [:ent-id :attribute :value])
        (update :ent-id read-string)))

#_(defn build-verb [ctx]
    {:post [(keyword? %)]}
    (or (keyword (get-in ctx [:request :params :verb]))
        (get-in ctx [:request :request-method])))

#_(defn build-context [{:keys [request] :as ctx}]
    (let []
      (merge (-> (get-in ctx [:request :params])
                 (select-keys [:time :place]))
             {:creds (friend/current-authentication request)})))

(defn request-context->action-message
  "If for some reason the request is in some way logically incomplete,
  here's the place to indicate that."
  ([ctx]
   (let [body (util/transit-read (get-in ctx [:request :body]))
         params (get-in ctx [:request :params])]
     (->> (merge body params)
          (reduce (fn [acc [k v]]
                    (if (clojure.string/blank? v)
                      (assoc acc k {})
                      (assoc acc k (read-string v))))
                  {})))))
