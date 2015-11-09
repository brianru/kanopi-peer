(ns kanopi.view.resources.spa
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :as rep]
            [cemerick.friend :as friend]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.view.resources.base :as base]
            [kanopi.view.resources.templates :as html]))

(defn spa [ctx]
  (let [user-data   (or (friend/current-authentication (:request ctx))
                        (authenticator/temp-user))
        session-svc (util/get-session-service ctx)]
    (html/om-page
     ctx
     {:title  "kanopi"
      :cookie (session/init-session session-svc user-data)
      })))

(defresource spa-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
