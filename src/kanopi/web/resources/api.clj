(ns kanopi.web.resources.api
  "Glue api routes to database component."
  (:require [liberator.core :refer (defresource)]
            [liberator.representation :refer (ring-response)]
            [kanopi.web.resources.base :as base]
            [cemerick.friend :as friend]
            [kanopi.data :as data]
            [kanopi.util.core :as util]
            ))

(defn query-db [data-fn path]
  (fn [ctx]
    (data-fn (util/get-data-service ctx)
             (get-in ctx path))))

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

(defn generate-full-message [ctx]
  (hash-map ::message {:noun    (build-noun ctx)
                       :verb    (build-verb ctx)
                       :context (build-context ctx)}))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :exists? generate-full-message
  ;;:exists? (query-db data/get-thunk     [:request :params :id])
  ;;:put!    (query-db data/swap-entity   [:request :params :entity])
  ;;:delete! (query-db data/retract-thunk [:request :params :id])
  ;;:patch!  (query-db data/assert-statements [:request :params :statements])
  ;;:post!   (query-db data/add-entity        [:request :params :entity])

  :new? ::new
  :respond-with-entity? true
  :handle-ok (fn [ctx] (str (get ctx ::message)))
  )
