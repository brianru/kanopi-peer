(ns nebula.web.auth
  (:require [liberator.core :refer [defresource]]))

(defresource authentication-resource
  :allowed-methods [:get :put :post :delete]
  :available-media-types ["text/html"]
  )
