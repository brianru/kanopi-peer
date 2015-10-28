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

(defn get-value
  ([m]
   (get-value m ""))
  ([m default-value]
   (get m (get default-value-key (describe-entity m)) default-value)))

(defn display-entity [m]
  (get-value m "help, I'm trapped!"))

(defn create-entity [tp value]
  (hash-map
   :db/id nil
   (get default-value-key tp) value))

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
   (s/optional-key :datum/fact) [(s/recursive #'Fact)]
   })

;; NOTE: my expression of Literal types is not pretty, abstract-maps
;; look helpful, but I do not want an explcit type encoded in the map
;; besides the presence of the literal/<tp> key. there's no reason to
;; encode this information twice. it only confuses things as datomic
;; requires typed attributes.
(s/defschema TextLiteral
  {:db/id (s/maybe DatomicId)
   :literal/text s/Str})

(s/defschema IntegerLiteral
  {:db/id (s/maybe DatomicId)
   :literal/integer s/Int})

(s/defschema Literal
  (s/conditional
   #(find % :literal/text)    TextLiteral
   #(find % :literal/integer) IntegerLiteral)
   )

