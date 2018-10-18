(ns kanopi.view.resources.api
  "Synchronous message passing api."
  (:require [liberator.core :refer (defresource)]
            [liberator.representation :refer (ring-response)]
            [kanopi.view.resources.base :as base]))


(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/json"]

  ;; :processable? (fn [ctx]
  ;;                 (hash-map ::message (server-msg/remote->local ctx)))
  :handle-unprocessable-entity "The request is in some way incomplete or illogical."

  ;; :post! (fn [ctx]
  ;;          (->> (get ctx ::message)
  ;;               (handlers/request-handler ctx)
  ;;               (hash-map ::response)))

  :new? false
  :respond-with-entity? true
  :post-redirect? false

  :handle-ok (fn [ctx]
               (get-in ctx [::response])))
