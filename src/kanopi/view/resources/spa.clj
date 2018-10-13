(ns kanopi.view.resources.spa
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :as rep]
            [cemerick.friend :as friend]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.model.session :as session]
            [kanopi.view.resources.base :as base]
            [kanopi.view.resources.templates :as html]
            [kanopi.util.core :as util]))

(defn spa [ctx]
  (let [user-data   (friend/current-authentication (:request ctx))
        session-svc (util/get-session-service ctx)]
    (html/om-page
     ctx
     {:title  "kanopi"
      :session-state (if user-data
                       (session/init-session session-svc user-data))})))

(defresource spa-resource
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok spa)
