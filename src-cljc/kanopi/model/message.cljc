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
                       [schema.experimental.abstract-map :as abstract-map
                        :include-macros true]
                       [kanopi.controller.history :as history]
                       [taoensso.timbre :as timbre
                        :refer-macros (log trace debug info warn error fatal report)]
                       [ajax.core :as ajax]
                       [cljs.core.async :as async] 
                       [kanopi.model.schema :as schema]
                       [kanopi.util.core :as util]
                       ]
                :clj  [[clojure.string]
                       [schema.core :as s]
                       [schema.experimental.abstract-map :as abstract-map]
                       [cemerick.friend :as friend]
                       [taoensso.timbre :as timbre
                        :refer (log trace debug info warn error fatal report)]
                       [kanopi.model.schema :as schema]
                       [kanopi.util.core :as util]
                       ])))

;; Pure cross-compiled message generators
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/defschema Message
  (abstract-map/abstract-map-schema
   :verb
   {:noun    schema/Noun
    :verb    schema/Verb
    :context schema/Context
    :tx/id   s/Str}))

(s/defn message :- Message
  [& args]
  (let [{:keys [noun verb context]
         :or {noun {} context {}}}
        (apply hash-map args)]
    (hash-map
     :tx/id (util/random-uuid)
     :noun noun
     :verb verb
     :context context)))

(abstract-map/extend-schema GetDatum Message
                            [:datum/get]
                            {:noun schema/DatomicId})
(s/defn get-datum :- GetDatum
  [datum-id]
  (message :noun datum-id :verb :datum/get))

(abstract-map/extend-schema AddFact Message
                            [:datum.fact/add]
                            {:noun {:datum-id schema/DatomicId}})
(s/defn add-fact :- AddFact
  [datum-id]
  (message :noun {:datum-id datum-id}
           :verb :datum.fact/add))

(abstract-map/extend-schema UpdateFact Message
                            [:datum.fact/update]
                            {:noun {:datum-id schema/DatomicId
                                    :fact     schema/Fact}})
(s/defn update-fact :- UpdateFact
  [datum-id fact]
  (message :noun {:datum-id datum-id
                  :fact fact}
           :verb :datum.fact/update))

(abstract-map/extend-schema UpdateDatumLabel Message
                            [:datum.label/update]
                            {:noun {:existing-entity schema/Datum
                                    :new-label       s/Str}})
(s/defn update-datum-label :- UpdateDatumLabel
  [ent new-label]
  (message :noun {:existing-entity ent, :new-label new-label}
           :verb :datum.label/update))

(abstract-map/extend-schema InitializeClientState Message
                            [:spa.state/initialize]
                            {:noun schema/Credentials})
(s/defn initialize-client-state :- InitializeClientState
  [user]
  (message :noun user, :verb :spa.state/initialize))

(abstract-map/extend-schema CreateDatum Message
                            [:datum/create]
                            {})
(s/defn create-datum :- CreateDatum
  ([]
   (message :verb :datum/create)))

(defn create-goal
  ([]
   (message :verb :goal.modal/open))
  ([txt]
   (message :noun {:goal txt}
            :verb :goal/create)))

(defn record-insight
  ([]
   (message :verb :insight.modal/open))
  ([txt]
   (message :noun {:insight txt}
            :verb :insight/record)))

(abstract-map/extend-schema Search Message
                            [:spa.navigate/search]
                            {:noun {:query-string schema/QueryString
                                    :entity-type  s/Keyword}})
(s/defn search :- Search
  ([q]
   (search q nil))
  ([q tp]
   (message :noun {:query-string q, :entity-type tp}
            :verb :spa.navigate/search)))


;; Server-only utilities for parsing messages out of ring request maps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#?(:clj
   (defn- request->noun [ctx noun]
     {:post [(or (integer? %) (keyword? %) (instance? java.lang.Long %) (map? %))]}
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
            ;; NOTE: keyword namespaces are stripped out by transit
            parsed-body (->> (merge body params)
                             (reduce (fn [acc [k v]]
                                       (cond
                                        (contains? #{:message/id :id} k)
                                        (assoc acc k v)

                                        (string? v)
                                        (if (clojure.string/blank? v)
                                          (assoc acc k {})
                                          (assoc acc k (util/try-read-string v))) 

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
      (send! owner (message :noun (get msg :verb)
                            :verb :intent/register
                            :context (get msg :context))))
    ))

#?(:cljs
   (defn switch-team [team-id]
     (message :noun team-id :verb :switch-team)))

#?(:cljs
   (defn toggle-fact-mode [ent]
     (message :noun [:fact (:db/id ent)]
              :verb :toggle-mode))
   )
#?(:cljs
   (defn select-fact-part-type [fact-id fact-part tp]
     (message :noun [:fact fact-id]
              :verb :select-fact-part-type
              :context {:fact-part fact-part
                        :value tp}))
   )
#?(:cljs
   (defn input-fact-part-value [fact-id fact-part input-value]
     (message :noun [:fact fact-id]
              :verb :input-fact-part-value
              :context {:fact-part fact-part
                        :value input-value}))
   )
#?(:cljs
   (defn select-fact-part-reference [fact-id fact-part selection]
     (message :noun [:fact fact-id]
              :verb :select-fact-part-reference
              :context {:fact-part fact-part
                        :selection selection}))
   )
#?(:cljs
   (defn register [creds]
     (message :noun creds :verb :spa/register)))
#?(:cljs
   (defn register-success [creds]
     (message :noun creds :verb :spa.register/success))
   )
#?(:cljs
   (defn register-failure [err]
     (message :noun err :verb :spa.register/failure))
   )
#?(:cljs
   (defn login [creds]
     (message :noun creds :verb :spa/login))
   )
#?(:cljs
   (defn login-success [creds]
     (message :noun creds :verb :spa.login/success))
   )
#?(:cljs
   (defn login-failure [err]
     (message :noun err :verb :spa.login/failure))
   )
#?(:cljs
   (defn logout []
     (message :verb :spa/logout))
   )
#?(:cljs
   (defn logout-success [foo]
     (message :noun foo :verb :spa.logout/success))
   )
#?(:cljs
   (defn logout-failure [err]
     (message :noun err :verb :spa.logout/failure))
   )

#?(:cljs
   (defn valid-remote-message?
     "Some simple assertions on the shape of the remote message.
     It's not as willy-nilly as local messages, though that must change
     as well."
     [msg]
     (s/validate schema/RemoteMessage (get msg :noun)))
   )

#?(:cljs
   (do
    (defmulti local->remote
      (fn [history app-state msg]
        (info msg)
        (get msg :verb))
      :default :default)

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
      (standard-api-post history msg))

    (defmethod local->remote :search
      [history app-state msg]
      (standard-api-post history msg))

(defmethod local->remote :datum/create
  [history app-state msg]
  (standard-api-post history msg))

(defmethod local->remote :datum/get
  [history app-state msg]
  (standard-api-post history msg))

(defmethod local->remote :datum.label/update
  [history app-state msg]
  (standard-api-post history msg))

(defmethod local->remote :datum.fact/update
  [history app-state msg]
  (standard-api-post history msg))

))
