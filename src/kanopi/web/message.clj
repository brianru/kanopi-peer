(ns kanopi.web.message
  (:require [kanopi.util.core]
            [cemerick.friend :as friend]))

(defn build-noun [ctx]
  (-> ctx
      (get-in [:request :params])
      (select-keys [:ent-id :attribute :value])))

(defn build-verb [ctx]
  (or (get-in ctx [:request :params :verb])
      (get-in ctx [:request :request-method])))

(defn build-context [{:keys [request] :as ctx}]
  (let []
    (merge (-> (get-in ctx [:request :params])
               (select-keys [:time :place]))
           {:creds (friend/current-authentication request)})))

(defn request-context->message
  "If for some reason the request is in some way logically incomplete,
  here's the place to indicate that."
  [ctx]
  (hash-map ::message {:noun    (build-noun ctx)
                       :verb    (build-verb ctx)
                       :context (build-context ctx)}))
