(ns nebula.web.api
  (:require [liberator.core :refer [defresource]]))

(defresource api-resource
  :allowed-methods [:get :put :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :new? ::new
  :respond-with-entity? true
  :handle-ok ::data
  )
