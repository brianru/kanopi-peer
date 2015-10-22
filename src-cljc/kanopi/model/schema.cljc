(ns kanopi.model.schema
  (:require 
             #?@(:clj  [[schema.core :as s]
                        [schema.experimental.complete :as c]
                        [schema.experimental.generators :as g]]
                 :cljs [[schema.core :as s :include-macros true]]
                 )
            ))

(defn describe-entity [m]
  (let [ks (keys m)]
    (cond
     (some #{:datum/fact :datum/label :datum/role} ks)
     :datum

     (some #{:fact/attribute :fact/value} ks)
     :fact

     (some (comp #{"literal"} namespace) ks)
     :literal

     :default
     :unknown)))

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
  (s/conditional #(> (count %) 3) s/Str))

(s/defschema UserPassword
  (s/conditional #(> (count %) 8) s/Str))

(s/defschema UserCredentials
  [(s/one UserId "id") (s/one UserPassword "pw")])

(s/defschema UserRole
  {
   :db/id     (s/maybe DatomicId)
   :role/id    UserId
   :role/label UserId
   })

(s/defschema User
  {
   :db/id         (s/maybe DatomicId)
   :user/id       UserId
   :user/password UserPassword
   :user/role     UserRole
   })

(declare Datum Literal)

(s/defschema Fact
  ""
  {:db/id          (s/maybe DatomicId)
   :fact/attribute (s/cond-pre (s/recursive #'Datum)
                               Literal)
   :fact/value     (s/cond-pre (s/recursive #'Datum)
                               Literal)}
  )

(s/defschema Datum
  ""
  {:db/id       (s/maybe DatomicId)
   :datum/role  UserRole
   :datum/label (s/maybe s/Str)
   :datum/fact  [(s/recursive #'Fact)]
   })

(s/defschema TextLiteral
  {:literal/text s/Str})

(s/defschema Literal
  ""
  {:db/id      (s/maybe DatomicId)
   }

   )
