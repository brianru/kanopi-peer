(ns kanopi.model.message
  "Messages should have unique ids and be recorded in a massive k-v
  store. The id must include the following information:
  - user
  - session/ip
  - timestamp

  This is what will allow us to drive all major analytics. Every
  action the user takes stored for analysis and testing.

  "
  (:require #?@(:cljs [[om.core :as om]
                       [schema.core :as s :include-macros true]
                       [schema.experimental.abstract-map :as abstract-map
                        :include-macros true]
                       [taoensso.timbre :as timbre
                        :refer-macros (log trace debug info warn error fatal report)]
                       [cljs.core.async :as async] 
                       [kanopi.model.schema :as schema]
                       [kanopi.util.core :as util]
                       ]
                :clj  [
                       [schema.core :as s]
                       [schema.experimental.abstract-map :as abstract-map]
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
    ; :saga/id   s/Str
    }))

(s/defn message :- Message
  [& args]
  (let [{:keys [noun verb context]
         :or {noun {} context {}}}
        (apply hash-map args)]
    (hash-map
     ; :saga/id (util/random-uuid)
     :noun noun
     :verb verb
     :context context)))

; NOTE: for implementation of 'sagas' throughout the system to
; representing transaction semantics for user intentions
(s/defn response :- Message
  [request & args]
  (let [{:keys [noun verb context]
         :or {noun {} context {}}}
        (apply hash-map args)]
    (hash-map
     ; :saga/id (get request :saga/id)
     :noun noun
     :verb verb
     :context context
     )))

(abstract-map/extend-schema GetDatum Message
  [:datum/get]
  {:noun schema/DatomicId})
(s/defn get-datum :- GetDatum
  [datum-id]
  (message :noun datum-id :verb :datum/get))
(abstract-map/extend-schema GetDatumSuccess Message
  [:datum.get/success]
  {:noun schema/CurrentDatum})
(defn get-datum-success
  [user-datum]
  (message :noun user-datum
           :verb :datum.get/success))

(abstract-map/extend-schema GetLiteral Message
  [:literal/get]
  {:noun schema/DatomicId})
(s/defn get-literal :- GetLiteral
  [literal-id]
  (message :noun literal-id :verb :literal/get))
(abstract-map/extend-schema GetLiteralSuccess Message
  [:literal.get/success]
  {:noun schema/CurrentLiteral})
(defn get-literal-success
  [user-literal]
  (message :noun user-literal
           :verb :literal.get/success))

(abstract-map/extend-schema AddFact Message
  [:datum.fact/add]
  {:noun {:datum-id schema/DatomicId
          :fact     schema/Fact}})
(defn add-fact
  [datum-id fact]
  (message :noun {:datum-id datum-id
                  :fact fact}
           :verb :datum.fact/add))
(defn add-fact-success
  [datum new-entities]
  (message :noun {:datum datum
                  :new-entities new-entities}
           :verb :datum.fact.add/success))

(abstract-map/extend-schema UpdateFact Message
  [:datum.fact/update]
  {:noun {:datum-id schema/DatomicId
          :fact     schema/Fact}})
(defn update-fact
  [datum-id fact]
  (message :noun {:datum-id datum-id
                  :fact     fact}
           :verb :datum.fact/update))
; (s/defn update-fact :- UpdateFact
;   [datum-id fact]
;   (message :noun {:datum-id datum-id
;                   :fact fact}
;            :verb :datum.fact/update))
(defn update-fact-success
  [datum new-entities]
  (message :noun {:datum datum
                  :new-entities new-entities}
           :verb :datum.fact.update/success))

(abstract-map/extend-schema UpdateDatumLabel Message
  [:datum.label/update]
  {:noun {:existing-entity schema/Datum
          :new-label       s/Str}})
(defn update-datum-label
  [ent new-label]
  (message :noun {:existing-entity ent
                  :new-label new-label}
           :verb :datum.label/update))
;; Not sure why this version is failing:
; (s/defn update-datum-label :- UpdateDatumLabel
;   [ent :- s/Any
;    new-label :- s/Any]
;   (message :noun {:existing-entity ent, :new-label new-label}
;            :verb :datum.label/update))

(abstract-map/extend-schema UpdateDatumLabelSuccess Message
  [:datum.label.update/success]
  {:noun schema/Datum})
(defn update-datum-label-success
  [datum']
  (message :noun datum'
           :verb :datum.label.update/success))

(abstract-map/extend-schema UpdateLiteral Message
  [:literal/update]
  {:noun {:literal-id schema/DatomicId
          :new-type   s/Keyword
          :new-value  s/Any}})
(defn update-literal
  [literal-id tp value]
  (message :noun {:literal-id literal-id
                  :new-type   tp
                  :new-value  value}
           :verb :literal/update))
(abstract-map/extend-schema UpdateLiteralSuccess Message
  [:literal.update/success]
  {:noun schema/Literal})
(defn update-literal-success
  [literal]
  (message :noun literal
           :verb :literal.update/success))

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
(abstract-map/extend-schema CreateDatumSuccess Message
  [:datum.create/success]
  {:noun schema/CurrentDatum})
(defn create-datum-success
  [user-datum]
  (message :noun user-datum
           :verb :datum.create/success))

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

(abstract-map/extend-schema Navigate Message
  [:spa/navigate]
  {:noun s/Any})
(s/defn navigate :- Navigate
  [match]
  (message :noun match, :verb :spa/navigate))
(defn navigate-success
  [page]
  (message :noun page, :verb :spa.navigate/success))

(abstract-map/extend-schema Search Message
  [:spa.navigate/search]
  {:noun {:query-string s/Str
          :entity-type  s/Keyword}})
(s/defn search :- Search
  ([q]
   (search q nil))
  ([q tp]
   (message :noun {:query-string q, :entity-type tp}
            :verb :spa.navigate/search)))
(abstract-map/extend-schema NavigateSearchSuccess Message
  [:spa.navigate.search/success]
  {:noun {:query-string s/Str
          :results [s/Any]}})
(defn navigate-search-success
  [query-string results]
  (message :noun {:query-string query-string
                  :results results}
           :verb :spa.navigate.search/success))

(abstract-map/extend-schema SwitchTeam Message
  [:spa/switch-team]
  {:noun schema/TeamId})
(defn switch-team [team-id]
  (message :noun team-id :verb :spa/switch-team))
(abstract-map/extend-schema SwitchTeamSuccess Message
  [:spa.switch-team/success]
  {:noun schema/Credentials})
(defn switch-team-success [user']
  (message :noun user' :verb :spa.switch-team/success))
(defn change-password [current-password new-password confirm-new-password]
  (message :noun {}
           :verb :user/change-password))
(defn change-password-success []
  (message :noun {}
           :verb :user.change-password/success))
(defn change-password-failure []
  (message :noun {}
           :verb :user.change-password/failure))

(defn register [creds]
  (message :noun creds :verb :spa/register))

(defn register-success [creds]
  (message :noun creds :verb :spa.register/success))

(defn register-failure [err]
  (message :noun err :verb :spa.register/failure))

(defn login [creds]
  (message :noun creds :verb :spa/login))

(defn login-success [creds]
  (message :noun creds :verb :spa.login/success))

(defn login-failure [err]
  (message :noun err :verb :spa.login/failure))

(defn logout []
  (message :verb :spa/logout))

(defn logout-success [foo]
  (message :noun foo :verb :spa.logout/success))

(defn logout-failure [err]
  (message :noun err :verb :spa.logout/failure))

;; Client only helpers
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
       ; NOTE: for sagas!
       (om/update-state! owner ::sagas #(conj % (:saga/id msg)))
       ;; NOTE: js evt handlers don't like `false` as a return value, which
       ;; async/put! often returns. So we add a nil.
       nil)
      ([owner & msgs]
       (async/onto-chan (publisher owner) msgs false)
       ; NOTE: for sagas!
       (om/update-state! owner ::sagas #(apply conj % (map :sada/id msgs)))
       ))

    ; (defn register-intent!
    ;   "Ex: (->> (msg/update-label) (msg/register-intent! owner))"
    ;   [owner msg]
    ;   (send! owner (message :noun (get msg :verb)
    ;                         :verb :intent/register
    ;                         :context (get msg :context))))
    ))
