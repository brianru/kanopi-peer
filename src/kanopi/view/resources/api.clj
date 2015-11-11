(ns kanopi.view.resources.api
  "Synchronous message passing api.
  TODO: send all messages to Kafka then let Onyx handle them."
  (:require [liberator.core :refer (defresource)]
            [liberator.representation :refer (ring-response)]
            [kanopi.view.resources.base :as base]
            [cemerick.friend :as friend]
            [kanopi.model.data :as data]
            [kanopi.util.core :as util]
            [kanopi.model.message :as msg]
            [kanopi.controller.handlers :as handlers]
            ))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :processable? (fn [ctx]
                  (hash-map ::message (msg/remote->local ctx))) 
  :handle-unprocessable-entity "The request is in some way incomplete or illogical."

  ;; :exists? (fn [ctx]
  ;;            (let [data-svc (util/get-data-service ctx)
  ;;                  msg      (get ctx ::message)]
  ;;              (hash-map ::entity (data/user-datum data-svc msg))))

  :post! (fn [ctx]
           (->> (get ctx ::message)
                (handlers/request-handler ctx)
                (hash-map ::response)))

  :new? false
  :respond-with-entity? true
  :post-redirect? false

  :handle-ok (fn [ctx]
               (get-in ctx [::response]))
  )
