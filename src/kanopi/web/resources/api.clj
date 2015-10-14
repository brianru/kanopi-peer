(ns kanopi.web.resources.api
  "Synchronous message passing api.
  TODO: send all messages to Kafka then let Onyx handle them."
  (:require [liberator.core :refer (defresource)]
            [liberator.representation :refer (ring-response)]
            [kanopi.web.resources.base :as base]
            [cemerick.friend :as friend]
            [kanopi.data :as data]
            [kanopi.util.core :as util]
            [kanopi.web.message :as msg]
            [kanopi.web.handlers :as handlers]
            ))

(defresource api-resource base/requires-authentication
  :allowed-methods [:get :put :patch :post :delete]
  :available-media-types ["application/transit+json"
                          "application/edn"
                          "application/json"]

  :processable? (fn [ctx]
                  (hash-map ::message (msg/request-context->action-message ctx))) 
  :handle-unprocessable-entity "The request is in some way incomplete or illogical."

  ;; :exists? (fn [ctx]
  ;;            (let [data-svc (util/get-data-service ctx)
  ;;                  msg      (get ctx ::message)]
  ;;              (hash-map ::entity (data/user-datum data-svc msg))))

  :post! (fn [ctx]
           (->> (get ctx ::message)
                (handlers/request-handler ctx)
                (hash-map ::result)))

  :new? false
  :respond-with-entity? true
  :post-redirect? false

  :handle-ok (fn [ctx]
               (get-in ctx [::result]))
  )
