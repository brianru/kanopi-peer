(ns nebula.web.resources.api
  "Glue api routes to database component."
  (:require [liberator.core :refer (defresource)]
            [nebula.web.resources.base :as base]
            [nebula.data :as data]
            ))

(defn query-db [data-fn path]
  (fn [ctx]
    (data-fn (util/get-database ctx)
             (get-in ctx path))))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :exists? (query-db data/get-entity        [:request :params :id])
  :put!    (query-db data/swap-entity       [:request :params :entity])
  :delete! (query-db data/retract-entity    [:request :params :id])
  :patch!  (query-db data/assert-statements [:request :params :statements])
  :post!   (query-db data/add-entity        [:request :params :entity])

  :new? ::new
  :respond-with-entity? true
  :handle-ok ::data
  )
