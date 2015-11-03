(ns kanopi.model.schema
  (:require 
             #?@(:clj  [[schema.core :as s]
                        [schema.experimental.complete :as c]
                        [schema.experimental.generators :as g]
                        [schema.experimental.abstract-map :as m]]
                 :cljs [[schema.core :as s :include-macros true]
                        [schema.experimental.abstract-map :as m :include-macros true]]
                 )
            ))

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

(def default-value-key
  {:datum :datum/label
   :literal :literal/text})

(def literal-meta-keys
  #{:db/id :literal/team})

(defn get-value
  ([m]
   (get-value m ""))
  ([m default-value]
   (case (describe-entity m)
     :datum
     (get m :datum/label default-value)
     :literal
     (-> (apply dissoc m literal-meta-keys) (vals) (first) (or default-value))
     :unknown
     default-value)))

(defn display-entity [m]
  (get-value m "help, I'm trapped!"))

(defn create-entity [tp value]
  (hash-map
   :db/id nil
   (get default-value-key tp) value))

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

(s/defschema UserId
  (s/conditional #(>= (count %) 3) s/Str))

(s/defschema UserPassword
  (s/conditional #(>= (count %) 8) s/Str))

(s/defschema InputCredentials
  [(s/one UserId "id") (s/one UserPassword "pw")])

(s/defschema UserTeam
  {
   :db/id     (s/maybe DatomicId)
   :team/id    UserId
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

;; NOTE: my expression of Literal types is not pretty, abstract-maps
;; look helpful, but I do not want an explcit type encoded in the map
;; besides the presence of the literal/<tp> key. there's no reason to
;; encode this information twice. it only confuses things as datomic
;; requires typed attributes.
(s/defschema TextLiteral
  {:db/id (s/maybe DatomicId)
   :literal/team UserTeam
   :literal/text s/Str})

(s/defschema IntegerLiteral
  {:db/id (s/maybe DatomicId)
   :literal/team UserTeam
   :literal/integer s/Int})

(s/defschema Literal
  (s/conditional
   #(find % :literal/text)    TextLiteral
   #(find % :literal/integer) IntegerLiteral))

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
   :message/id s/Str
   })

