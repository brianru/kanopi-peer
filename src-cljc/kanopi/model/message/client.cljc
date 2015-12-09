(ns kanopi.model.message.client
  (:require [schema.core :as s]

            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]

            [ajax.core :as ajax]

            [kanopi.controller.history :as history]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]))

(defmulti local->remote
  (fn [history app-state msg]
    (info msg)
    (get msg :verb))
  :default :default)

(defn valid-remote-message?
  "Some simple assertions on the shape of the remote message.
  It's not as willy-nilly as local messages, though that must change
  as well."
  [msg]
  (s/validate schema/RemoteMessage (get msg :noun)))

(defn standard-api-post [history msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :api)
          :params          (select-keys msg [:noun :verb :context])
          :method          :post
          :format          :transit
          :response-format :transit
          :response-method :aether
          :error-method    :aether}
   :verb :request
   :context {}))


(defmethod local->remote :spa/register
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :register)
          :params          (get msg :noun)
          :method          :post
          :response-format :transit
          :response-method :aether
          ;; NOTE: here xforms are specified because the server
          ;; does not respond with a message, but we want to use
          ;; aether so we must transform it to a message
          :response-xform  msg/register-success
          :error-method    :aether
          :error-xform     msg/register-failure
          }
   :verb :request
   :context {}))

(defmethod local->remote :spa/login
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {
          ;; NOTE: cljs-ajax parses params to req body for POST
          ;; requests. friend auth lib requires username and password
          ;; to appear in params or form-params, not body.
          :uri             (ajax/uri-with-params
                            (history/get-route-for history :login)
                            (get msg :noun))
          :method          :post
          :response-format :transit
          :response-method :aether
          :response-xform  msg/login-success
          :error-method    :aether
          :error-xform     msg/login-failure
          }
   :verb :request
   :context {}))


(defmethod local->remote :spa/logout
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :logout)
          :method          :post
          :response-format :transit
          :response-method :aether
          :response-xform  msg/logout-success
          :error-method    :aether
          :error-xform     msg/logout-failure
          }
   :verb :request
   :context {}))

(defmethod local->remote :default
  [history app-state msg]
  (standard-api-post history msg))

; (defmethod local->remote :spa.state/initialize
;   [history app-state msg]
;   (standard-api-post history msg))

; (defmethod local->remote :spa.navigate/search
;   [history app-state msg]
;   (standard-api-post history msg))

; (defmethod local->remote :datum/create
;   [history app-state msg]
;   (standard-api-post history msg))

; (defmethod local->remote :datum/get
;   [history app-state msg]
;   (standard-api-post history msg))

; (defmethod local->remote :datum.label/update
;   [history app-state msg]
;   (standard-api-post history msg))

; (defmethod local->remote :datum.fact/update
;   [history app-state msg]
;   (standard-api-post history msg))

