(ns kanopi.model.message
  (:require [om.core :as om]
            [kanopi.controller.history :as history]
            [cljs.core.async :as async]))

(defn publisher [owner]
  (om/get-shared owner [:aether :publisher]))

;; TODO: make work with different first args
;; eg. a core.async channel, an aether map, an aether record, etc
(defn send!
  "Ex: (->> (msg/search \"foo\") (msg/send! owner))
  TODO: allow specification of transducers or debounce msec via args
        - otherwise user must create some extra wiring on their end,
          which is what this fn is trying to avoid. layered abstractions."
  ([owner msg & args]
   (async/put! (publisher owner) msg)
   ;; NOTE: js evt handlers don't like `false` as a return value, which
   ;; async/put! often returns. So we add a nil.
   nil
   ))

(defn get-thunk [thunk-id]
  {:pre [(integer? thunk-id)]}
  (hash-map
   :noun thunk-id
   :verb :get-thunk
   :context {}))

;; local component message
(defn toggle-fact-mode [ent]
  (hash-map
   :noun [:fact (:db/id ent)]
   :verb :toggle-mode
   :context {}))

;; local component message
(defn select-fact-part-type [fact-id fact-part tp]
  (hash-map
   :noun [:fact fact-id]
   :verb :select-fact-part-type
   :context {:fact-part fact-part
             :value tp}))

;; local component message
(defn input-fact-part-value [fact-id fact-part input-value]
  (hash-map
   :noun [:fact fact-id]
   :verb :input-fact-part-value
   :context {:fact-part fact-part
             :value input-value}))

;; local component message
(defn select-fact-part-reference [fact-id fact-part selection]
  (hash-map
   :noun [:fact fact-id]
   :verb :select-fact-part-reference
   :context {:fact-part fact-part
             :selection selection}))

(defn update-fact [thunk-id fact]
  (hash-map
   :noun {:thunk-id thunk-id
          :fact fact}
   :verb :update-fact
   :context {}))

(defn update-thunk-label [ent new-label]
  (hash-map
   :noun {:existing-entity ent
          :new-label new-label}
   :verb :update-thunk-label
   :context {}))

(defn search
  ([q]
   (search q nil))
  ([q tp]
   (hash-map
    :noun {:query-string q
           :entity-type  tp}
    :verb :search
    :context {})))

(defn register [creds]
  (hash-map
   :noun creds
   :verb :register
   :context {}))

(defn register-success [creds]
  (hash-map
   :noun creds
   :verb :register-success
   :context {}))

(defn register-failure [err]
  (hash-map
   :noun err
   :verb :register-failure
   :context {}))

(defn login [creds]
  (hash-map
   :noun creds
   :verb :login
   :context {}))

(defn login-success [creds]
  (hash-map
   :noun creds
   :verb :login-success
   :context {}))

(defn login-failure [err]
  (hash-map
   :noun err
   :verb :login-failure
   :context {}))

(defn logout []
  (hash-map
   :noun nil
   :verb :logout
   :context {}))

(defn logout-success [foo]
  (hash-map
   :noun foo
   :verb :logout-success
   :context {}))

(defn logout-failure [err]
  (hash-map
   :noun err
   :verb :logout-failure
   :context {}))

(defn valid-remote-message?
  "Some simple assertions on the shape of the remote message.
  It's not as willy-nilly as local messages, though that must change
  as well."
  [msg]
  (-> msg
      (get :noun)
      ((juxt :uri :method :response-method :error-method))
      (->> (every? identity))))

(defmulti local->remote
  (fn [history app-state msg]
    (println "local->remote" msg)
    (get msg :verb))
  :default :default)

(defmethod local->remote :register
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :register)
          :params          (get msg :noun)
          :method          :post
          :response-format :transit
          :response-method :aether
          :response-xform  register-success
          :error-method    :aether
          :error-xform     register-failure
          }
   :verb :request
   :context {}))

(defmethod local->remote :login
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :login)
          :params          (get msg :noun)
          :method          :post
          :response-format :transit
          :response-method :aether
          :response-xform  login-success
          :error-method    :aether
          :error-xform     login-failure
          }
   :verb :request
   :context {}))

(defmethod local->remote :logout
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :logout)
          :method          :post
          :response-format :transit
          :response-method :aether
          :response-xform  logout-success
          :error-method    :aether
          :error-xform     logout-failure
          }
   :verb :request
   :context {}))

(defmethod local->remote :default
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map 
   :noun {:body msg
          :uri  (history/get-route-for history :api)
          :method :post
          :response-format :transit
          :response-method :aether
          :error-method    :aether
          }
   :verb :request
   :context {}))

(defmethod local->remote :get-thunk
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :api)
          :body            msg
          :method          :post
          :response-format :transit
          :response-method :aether
          :error-method    :aether
          }
   :verb :request
   :context {}))

;; TODO: implement
(defmethod local->remote :update-thunk-label
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {}
   :verb :request
   :context {}))

;; TODO: implement
(defmethod local->remote :update-fact
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {}
   :verb :request
   :context {}))
