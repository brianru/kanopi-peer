(ns nebula.web.resources.api
  (:require [liberator.core :refer [defresource]]
            [nebula.web.resources.base :as base]))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :new? ::new
  :respond-with-entity? true
  :handle-ok ::data
  )
