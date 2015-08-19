(ns kanopi.web.resources.api
  "Glue api routes to database component."
  (:require [liberator.core :refer (defresource)]
            [kanopi.web.resources.base :as base]
            [kanopi.data :as data]
            [kanopi.util.core :as util]
            ))

(defn query-db [data-fn path]
  (fn [ctx]
    (data-fn (util/get-datomic ctx)
             (get-in ctx path))))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :exists? (query-db data/get-thunk        [:request :params :id])
  :put!    (query-db data/swap-entity       [:request :params :entity])
  :delete! (query-db data/retract-thunk    [:request :params :id])
  ;;:patch!  (query-db data/assert-statements [:request :params :statements])
  ;;:post!   (query-db data/add-entity        [:request :params :entity])

  :new? ::new
  :respond-with-entity? true
  :handle-ok ::data
  )
