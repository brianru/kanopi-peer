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
                 )
            ))

; TODO: can I use transit to improve my parsing of these values?
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
                            :cljs cljs.reader/read-string))
  
    }

   ;; TODO: implement.
   ; :literal/uri
   ; {:ident :literal/uri
   ;  :predicate (constantly nil)}

   ;; TODO: implement.
   ; :literal/email-address
   ; {:ident :literal/email-address
   ;  :predicate (constantly nil)
   ;  }
   })

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
       (some #{:datum/fact :datum/label :datum/team} ks)
       :datum

       (some #{:fact/attribute :fact/value} ks)
       :fact

       (some (comp #{"literal"} namespace) ks)
       :literal

       :default
       :unknown))))

(defn datum? [m]
  (= :datum (describe-entity m)))
(defn fact? [m]
  (= :fact (describe-entity m)))
(defn literal? [m]
  (= :literal (describe-entity m)))

(def literal-meta-keys
  #{:db/id :literal/team})

(defn get-value-key
  ([ent]
   (get-value-key ent nil))
  ([ent default]
   (case (describe-entity ent)
    :datum
    :datum/label

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

;; TODO: flesh this out based on lucene search syntax
(s/defschema QueryString (s/conditional not-empty s/Str))

(s/defschema UserId
  (s/conditional #(>= (count %) 3) s/Str))

(s/defschema TeamId UserId)

(s/defschema UserPassword
  (s/conditional #(>= (count %) 8) s/Str))

(s/defschema InputCredentials
  [(s/one UserId "id") (s/one UserPassword "pw")])

(s/defschema UserTeam
  {
   :db/id   (s/maybe DatomicId)
   :team/id TeamId
   })

(s/defschema Credentials
  {
   :ent-id   DatomicId
   :username UserId
   :password UserPassword
   :teams    [UserTeam]
   :current-team UserTeam
   })

(s/defschema User
  {
   :db/id         (s/maybe DatomicId)
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
  {:db/id       (s/maybe DatomicId)
   :datum/team  UserTeam
   :datum/label (s/maybe s/Str)
   (s/optional-key :datum/fact)  [(s/recursive #'Fact)]
   })

(s/defschema NormalizedFact
  {:db/id DatomicId
   :fact/attribute DatomicId
   :fact/value DatomicId})

(s/defschema NormalizedDatum
  {:db/id (s/maybe DatomicId)
   :datum/team DatomicId
   :datum/label (s/maybe s/Str)
   :datum/fact [DatomicId]
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
  {:db/id (s/maybe DatomicId)
   :literal/team UserTeam
   :literal/text s/Str})

(s/defschema CodeLiteral
  {:db/id (s/maybe DatomicId)
   :literal/team UserTeam
   :literal/code s/Str})

(s/defschema IntegerLiteral
  {:db/id (s/maybe DatomicId)
   :literal/team UserTeam
   :literal/integer s/Int})

(s/defschema DecimalLiteral
  {:db/id (s/maybe DatomicId)
   :literal/team UserTeam
   :literal/decimal s/Num})

(s/defschema Literal
  (s/conditional
   #(find % :literal/text)    TextLiteral
   #(find % :literal/code)    CodeLiteral
   #(find % :literal/integer) IntegerLiteral
   #(find % :literal/decimal) DecimalLiteral))

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

(s/defschema AppState
  {
   :mode    s/Keyword
   :user    (s/maybe User)
   :page    s/Any
   :intent  s/Any
   :datum   (s/maybe CurrentDatum)
   :literal (s/maybe CurrentLiteral)
   :most-viewed-datums [s/Any]
   :most-edited-datums [s/Any]
   :recent-datums      [s/Any]
   :cache {s/Any s/Any}
   ; :cache {DatomicId (s/cond-pre NormalizedDatum NormalizedFact Literal}

   :search-results {s/Str [s/Any]}
   :error-messages [s/Any]
   :log [s/Any]
   })
