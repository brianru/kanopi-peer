(ns kanopi.model.schema
  (:require
             #?@(:clj  [[schema.core :as s]
                        [schema.experimental.complete :as c]
                        [schema.experimental.generators :as g]
                        clojure.set
                        [schema.experimental.abstract-map :as m]]
                 :cljs [[schema.core :as s :include-macros true]
                        [schema.experimental.abstract-map :as m :include-macros true]
                        clojure.set
                        cljs.reader]
                 )))

; TODO: can I use transit to improve my parsing of these values?
; TODO: why 2 fns? should the predicate be the parser, returning
; a map with either the value or the error type and relevant feedback
; TODO: consider modifying predicate to return useful error messages
; by separating error modes and checking each one instead of
; short-circuiting at the first failure, then return the set of error
; messages
(def literal-types
  {:literal/text
   {:ident :literal/text
    :label "text"
    :predicate string?
    :parser identity}

   :literal/math
   {:ident :literal/math
    :label "math"
    :predicate string?
    :parser identity}

   :literal/integer
   {:ident :literal/integer
    :label "integer"
    :predicate (fn [v]
                (or (integer? v)
                    (and (string? v) (re-find #"^[0-9]*$" v))))
    :parser    (comp int #?(:clj  read-string
                            :cljs cljs.reader/read-string))}

   :literal/decimal
   {:ident :literal/decimal
    :label "decimal"
    :predicate (fn [v]
                 (or
                  #?(:clj  (instance? java.lang.Double v)
                     :cljs (number? v))
                  (and (string? v)
                       (re-find #"^[0-9]?[0-9]*\.+[0-9]*$" v))))
    :parser (comp double #?(:clj  read-string
                            :cljs cljs.reader/read-string))}
    :literal/uri
    {:ident :literal/uri
     :label "uri"
     ; TODO: serious validation.
     :predicate (fn [v]
                  (and (string? v)
                       true))
     :parser identity}}) 

(def input-types
  (assoc literal-types
         :datum/label
         {:ident :datum/label
          :label "datum"
          :predicate string?
          :parser identity}))

(def renderable-types
  #{:literal/math})

(def fact-attribute-input-ordering
  (list :datum/label :literal/text))

(def fact-value-input-ordering
  (let [ordered-items   (list :datum/label :literal/text)
        remaining-items (clojure.set/difference (set input-types) (set ordered-items))]
    (concat ordered-items remaining-items)))

(def input-types-ordered-by-fact-part-preference
  {:fact/attribute (mapv #(get input-types %) fact-attribute-input-ordering) 
   :fact/value     (mapv #(get input-types %) fact-value-input-ordering)})

(defn describe-entity
  [m]
  (if-not (map? m) :unknown
    (let [ks (keys m)]
      (cond
       (some #{:user/id} ks)
       :user

       (some #{:team/id} ks)
       :team

       (some #{:datum/fact :datum/label :datum/team} ks)
       :datum

       (some #{:fact/attribute :fact/value} ks)
       :fact

       (some (comp #{"literal"} namespace) ks)
       :literal

       :default
       :unknown))))

(defn datum?   [m] (= :datum   (describe-entity m)))
(defn fact?    [m] (= :fact    (describe-entity m)))
(defn literal? [m] (= :literal (describe-entity m)))

(def literal-meta-keys
  #{:db/id :literal/team})

(defn get-value-key
  ([ent]
   (get-value-key ent nil))
  ([ent default]
   (case (describe-entity ent)
    :datum
    :datum/label

    ;; FIXME: this doesn't work. it's tricky, an attribute can either be a
    ;; literal or a datum, this is recursive, but is that data available at this
    ;; point?
    :fact
    :fact

    :literal
    (-> (apply dissoc ent literal-meta-keys) (keys) (first) (or default))

    :unknown
    default)))

(defn get-input-type [ent]
  (let [value-key-id (get-value-key ent)]
    (get input-types value-key-id)))

(defn get-value
  ([m]
   (get-value m ""))
  ([m default-value]
   (if-let [k (get-value-key m default-value)]
     (get m k default-value)
     default-value)
   ))

(defn compatible-input-types
  [input-value]
  (filter
   (fn [{:keys [ident label predicate] :as tp}]
     (cond
      (and (coll? input-value) (empty? input-value))
      true

      (predicate input-value)
      (predicate input-value)

      :default
      false))
   (vals input-types)))

(defn display-entity [m]
  (get-value m))

(defn user-default-team [creds]
  (let [username (get creds :username)]
    (->> creds
         :teams
         (filter (fn [rl]
                   (= (:username creds) (:team/id rl))))
         (first)
         :db/id)))

(defn current-team [creds]
  (get-in creds [:current-team :db/id]))

(s/defschema DatomicId s/Int)

(s/defschema DatomicIdInclTemp (s/cond-pre DatomicId s/Str))

;; TODO: flesh this out based on lucene search syntax
(s/defschema QueryString (s/conditional not-empty s/Str))

(def user-id-min-length 3)
(s/defschema UserId
  (s/conditional #(>= (count %) user-id-min-length)
                 s/Str))

(s/defschema TeamId UserId)

(def user-password-min-length 8)
(s/defschema UserPassword
  (s/conditional #(>= (count %) user-password-min-length)
                 s/Str))

(s/defschema InputCredentials
  [(s/one UserId "id") (s/one UserPassword "pw")])

(s/defschema UserTeam
  {
   :db/id   (s/maybe DatomicIdInclTemp)
   :team/id TeamId
   })

(s/defschema Credentials
  {
   :ent-id   DatomicIdInclTemp
   :username UserId
   (s/optional-key :password) UserPassword
   :teams    [UserTeam]
   :current-team UserTeam
   })

(s/defschema User
  {
   :db/id         (s/maybe DatomicIdInclTemp)
   :user/id       UserId
   :user/password UserPassword
   :user/team     UserTeam
   })

(declare Datum Literal)

(s/defschema Fact
  ""
  {:db/id (s/maybe DatomicId)

   :fact/attribute
   (s/conditional #(= :datum   (describe-entity %)) (s/recursive #'Datum)
                  #(= :literal (describe-entity %)) Literal)
   :fact/value
   (s/conditional #(= :datum   (describe-entity %)) (s/recursive #'Datum)
                  #(= :literal (describe-entity %)) Literal)
   })

(s/defschema Datum
  ""
  {:db/id       (s/maybe DatomicIdInclTemp)
   :datum/team  UserTeam
   :datum/label (s/maybe s/Str)
   (s/optional-key :datum/fact)  [(s/recursive #'Fact)]
   })

(s/defschema NormalizedFact
  {:db/id DatomicId
   :fact/attribute DatomicIdInclTemp
   :fact/value DatomicIdInclTemp})

(s/defschema NormalizedDatum
  {:db/id (s/maybe DatomicIdInclTemp)
   :datum/team DatomicIdInclTemp
   :datum/label (s/maybe s/Str)
   :datum/fact [DatomicIdInclTemp]
   })

(s/defschema CurrentDatum
  ""
  {:datum Datum
   :context-datums [Datum]
   :similar-datums [Datum]})

(s/defschema CurrentLiteral
  {:literal Literal
   :context-datums [Datum]})

;; NOTE: my expression of Literal types is not pretty, abstract-maps
;; look helpful, but I do not want an explcit type encoded in the map
;; besides the presence of the literal/<tp> key. there's no reason to
;; encode this information twice. it only confuses things as datomic
;; requires typed attributes.
(s/defschema TextLiteral
  {:db/id (s/maybe DatomicIdInclTemp)
   :literal/team UserTeam
   :literal/text s/Str})

(s/defschema MathLiteral
  {:db/id (s/maybe DatomicIdInclTemp)
   :literal/team UserTeam
   :literal/math s/Str})

(s/defschema UriLiteral
  {:db/id (s/maybe DatomicIdInclTemp)
   :literal/team UserTeam
   :literal/uri s/Str})

(s/defschema IntegerLiteral
  {:db/id (s/maybe DatomicIdInclTemp)
   :literal/team UserTeam
   :literal/integer s/Int})

(s/defschema DecimalLiteral
  {:db/id (s/maybe DatomicIdInclTemp)
   :literal/team UserTeam
   :literal/decimal s/Num})

(s/defschema Literal
  (s/conditional
   #(find % :literal/text)    TextLiteral
   #(find % :literal/math)    MathLiteral
   #(find % :literal/integer) IntegerLiteral
   #(find % :literal/decimal) DecimalLiteral
   #(find % :literal/uri)     UriLiteral))

(s/defschema Noun s/Any)

(s/defschema Verb s/Keyword)

(s/defschema Context s/Any)

;; Sent as another message's noun.
(s/defschema RemoteMessage
  {                             :uri  s/Str
                             :method  s/Keyword
            (s/optional-key :params)  s/Any
            (s/optional-key :format)  s/Keyword
   (s/optional-key :response-format)  s/Keyword
                    :response-method  s/Keyword
    (s/optional-key :response-xform)  (s/=> s/Any s/Any)
                       :error-method  s/Keyword
       (s/optional-key :error-xform)  (s/=> s/Any s/Any)
   })

(s/defschema Message
  {:noun       Noun
   :verb       Verb
   :context    Context
   :tx/id s/Str
   })

(s/defschema ClientCache
  {DatomicIdInclTemp
   (s/cond-pre NormalizedDatum NormalizedFact Literal UserTeam)})

(s/defschema AppState
  {
   :mode    s/Keyword
   :user    (s/maybe Credentials)
   :page    s/Any
   :intent  s/Any
   :datum   (s/maybe CurrentDatum)
   :literal (s/maybe CurrentLiteral)
   :most-viewed-datums [s/Any]
   :most-edited-datums [s/Any]
   :recent-datums      [s/Any]
   :cache ClientCache

   :search-results {s/Str [s/Any]}
   :error-messages [s/Any]
   :log [s/Any]
   })

(s/defschema ClientSession
  {(s/optional-key :mode) s/Keyword
   :user Credentials
   :page s/Str
   (s/optional-key :datum) CurrentDatum
   :cache ClientCache
   })
