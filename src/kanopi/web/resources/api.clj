(ns kanopi.web.resources.api
  "Glue api routes to database component."
  (:require [liberator.core :refer (defresource)]
            [liberator.representation :refer (ring-response)]
            [kanopi.web.resources.base :as base]
            [cemerick.friend :as friend]
            [kanopi.data :as data]
            [kanopi.util.core :as util]
            [kanopi.web.message :as msg]
            ))

(defn query-db [data-fn path]
  (fn [ctx]
    (data-fn (util/get-data-service ctx)
             (get-in ctx path))))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :processable? msg/request-context->message

  ;;:exists? (query-db data/get-thunk     [:request :params :id])
  ;;:put!    (query-db data/swap-entity   [:request :params :entity])
  ;;:delete! (query-db data/retract-thunk [:request :params :id])
  ;;:patch!  (query-db data/assert-statements [:request :params :statements])
  ;;:post!   (query-db data/add-entity        [:request :params :entity])

  :new? ::new
  :respond-with-entity? true
  :handle-ok (fn [ctx] (str (get ctx ::message)))
  :handle-unprocessable-entity "The request is in some way incomplete or illogical."
  )
