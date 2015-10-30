(ns kanopi.model.message
  "Messages should have unique ids and be recorded in a massive k-v
  store. The id must include the following information:
  - user
  - session/ip
  - timestamp

  This is what will allow us to drive all major analytics. Every
  action the user takes stored for analysis and testing.
  
  TODO: schematize all message creator fns
  "
  (:require #?@(:cljs [[om.core :as om]
                       [schema.core :as s :include-macros true]
                       [kanopi.controller.history :as history]
                       [taoensso.timbre :as timbre
                        :refer-macros (log trace debug info warn error fatal report)]
                       [ajax.core :as ajax]
                       [cljs.core.async :as async] 
                       ]
                :clj  [[clojure.string]
                       [schema.core :as s]
                       [cemerick.friend :as friend]
                       [taoensso.timbre :as timbre
                        :refer (log trace debug info warn error fatal report)]
                       [kanopi.model.schema :as schema]
                       [kanopi.util.core :as util]
                       ])))

;; Pure cross-compiled message generators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-datum [datum-id]
  {:pre [(integer? datum-id)]}
  (hash-map
   :noun datum-id
   :verb :datum/get
   :context {}))

(defn update-fact [datum-id fact]
  (hash-map
   :noun {:datum-id datum-id
          :fact fact}
   :verb :datum.fact/update
   :context {}))

(defn update-datum-label [ent new-label]
  (hash-map
   :noun {:existing-entity ent
          :new-label new-label}
   :verb :datum.label/update
   :context {}))

(defn initialize-client-state [user]
  (hash-map
   :noun user
   :verb :spa.state/initialize
   :context {}))


(defn search
  ([q]
   (search q nil))
  ([q tp]
   (hash-map
    :noun {:query-string q
           :entity-type  tp}
    :verb :spa.navigate/search
    :context {})))


;; Server-only utilities for parsing messages out of ring request maps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#?(:clj
   (defn- request->noun [ctx noun]
     {:post [(or (integer? %) (instance? java.lang.Long %) (map? %))]}
     noun))

#?(:clj
   (defn- request->verb [ctx verb]
     {:post [(keyword? %)]}
     verb))

#?(:clj
   (defn- request->context [request-context message-context]
     {:post [(map? %)]}
     (let [creds (-> (friend/current-authentication (:request request-context))
                     :identity
                     ((util/get-auth-fn request-context))
                     )]
       (s/validate schema/Credentials creds)
       (assoc message-context :creds creds))))

#?(:clj
   (defn remote->local
     "If for some reason the request is in some way logically incomplete,
     here's the place to indicate that."
     ([ctx]
      (let [body        (util/transit-read (get-in ctx [:request :body]))
            params      (get-in ctx [:request :params])
            parsed-body (->> (merge body params)
                             (reduce (fn [acc [k v]]
                                       (cond
                                        (string? v)
                                        (if (clojure.string/blank? v)
                                          (assoc acc k {})
                                          (assoc acc k (read-string v))) 

                                        :default
                                        (assoc acc k v)))
                                     {}))]
        (hash-map
         :noun    (request->noun    ctx (:noun    parsed-body))
         :verb    (request->verb    ctx (:verb    parsed-body))
         :context (request->context ctx (:context parsed-body)))))))

;; Client-only messages, aether helper fns, and local->remote
;; transformers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#?(:cljs
   (do
    (defn publisher [owner]
      (om/get-shared owner [:aether :publisher]))

    (defn send!
      "Ex: (->> (msg/search \"foo\") (msg/send! owner))
      TODO: allow specification of transducers or debounce msec via args
      - otherwise user must create some extra wiring on their end,
      which is what this fn is trying to avoid. layered abstractions.
      TODO: make work with different first args eg. a core.async channel,
      an aether map, an aether record, etc"
      ([owner msg]
       (async/put! (publisher owner) msg)
       ;; NOTE: js evt handlers don't like `false` as a return value, which
       ;; async/put! often returns. So we add a nil.
       nil)
      ([owner & msgs]
       (async/onto-chan (publisher owner) msgs false)
       ))

    (defn register-intent!
      "Ex: (->> (msg/update-label) (msg/register-intent! owner))"
      [owner msg]
      (send! owner (hash-map :noun (get msg :verb)
                             :verb :intent/register
                             :context (get msg :context))))
    ))

#?(:cljs
   (defn switch-team [team-id]
     (hash-map
      :noun team-id
      :verb :switch-team
      :context {})))

#?(:cljs
   (defn toggle-fact-mode [ent]
     (hash-map
      :noun [:fact (:db/id ent)]
      :verb :toggle-mode
      :context {}))
   )
#?(:cljs
   (defn select-fact-part-type [fact-id fact-part tp]
     (hash-map
      :noun [:fact fact-id]
      :verb :select-fact-part-type
      :context {:fact-part fact-part
                :value tp}))
   )
#?(:cljs
   (defn input-fact-part-value [fact-id fact-part input-value]
     (hash-map
      :noun [:fact fact-id]
      :verb :input-fact-part-value
      :context {:fact-part fact-part
                :value input-value}))
   )
#?(:cljs
   (defn select-fact-part-reference [fact-id fact-part selection]
     (hash-map
      :noun [:fact fact-id]
      :verb :select-fact-part-reference
      :context {:fact-part fact-part
                :selection selection}))
   )
#?(:cljs
   (defn register [creds]
     (hash-map 
      :noun creds
      :verb :spa/register
      :context {})))
#?(:cljs
   (defn register-success [creds]
     (hash-map
      :noun creds
      :verb :spa.register/success
      :context {}))
   )
#?(:cljs
   (defn register-failure [err]
     (hash-map
      :noun err
      :verb :spa.register/failure
      :context {}))
   )
#?(:cljs
   (defn login [creds]
     (hash-map
      :noun creds
      :verb :spa/login
      :context {}))
   )
#?(:cljs
   (defn login-success [creds]
     (hash-map
      :noun creds
      :verb :spa.login/success
      :context {}))
   )
#?(:cljs
   (defn login-failure [err]
     (hash-map
      :noun err
      :verb :spa.login/failure
      :context {}))
   )
#?(:cljs
   (defn logout []
     (hash-map
      :noun nil
      :verb :spa/logout
      :context {}))
   )
#?(:cljs
   (defn logout-success [foo]
     (hash-map
      :noun foo
      :verb :spa.logout/success
      :context {}))
   )
#?(:cljs
   (defn logout-failure [err]
     (hash-map
      :noun err
      :verb :spa.logout/failure
      :context {}))
   )

#?(:cljs
   (defn valid-remote-message?
     "Some simple assertions on the shape of the remote message.
     It's not as willy-nilly as local messages, though that must change
     as well."
     [msg]
     (-> msg
         (get :noun)
         ((juxt :uri :method :response-method :error-method))
         (->> (every? identity))))
   )

#?(:cljs
   (do
    (defmulti local->remote
      (fn [history app-state msg]
        (info msg)
        (get msg :verb))
      :default :default)

    (defmethod local->remote :spa/register
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
              :response-xform  login-success
              :error-method    :aether
              :error-xform     login-failure
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
       :noun {:uri  (history/get-route-for history :api)
              :body msg
              :method :post
              :response-format :transit
              :response-method :aether
              :error-method    :aether
              }
       :verb :request
       :context {}))

    (defmethod local->remote :spa.state/initialize
      [history app-state msg]
      {:post [(valid-remote-message? %)]}
      (hash-map
       :noun {:uri             (history/get-route-for history :api)
              :params          msg
              :format          :transit
              :method          :post
              :response-format :transit
              :response-method :aether
              :error-method    :aether}
       :verb :request
       :context {}))

    (defmethod local->remote :search
      [history app-state msg]
      {:post [(valid-remote-message? %)]}
      (hash-map
       :noun {:uri             (history/get-route-for history :api)
              :params          msg
              :format          :transit
              :method          :post
              :response-format :transit
              :response-method :aether
              :error-method    :aether}
       :verb :request
       :context {}))

(defmethod local->remote :get-datum
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :api)
          :params          msg
          :format          :transit
          :method          :post
          :response-format :transit
          :response-method :aether
          :error-method    :aether
          }
   :verb :request
   :context {}))

(defmethod local->remote :datum.label/update
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :api)
          :params          msg
          :format          :transit
          :method          :post
          :response-format :transit
          :response-method :aether
          :error-method    :aether
          }
   :verb :request
   :context {}))

(defmethod local->remote :datum.fact/update
  [history app-state msg]
  {:post [(valid-remote-message? %)]}
  (hash-map
   :noun {:uri             (history/get-route-for history :api)
          :params          msg
          :method          :post
          :format          :transit
          :response-format :transit
          :response-method :aether
          :error-method    :aether}
   :verb :request
   :context {}))

))
