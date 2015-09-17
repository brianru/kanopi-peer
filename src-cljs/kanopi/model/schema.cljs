(ns kanopi.model.schema
  (:require [cljs-uuid-utils.core :refer (make-random-uuid)]
            ))

(defn describe-entity [m]
  (cond
   (contains? m :thunk/label)
   :thunk
   (contains? m :fact/attribute)
   :fact
   (contains? m :value/string)
   :literal/text

   :default
   :unknown))

(defn thunk? [m]
  (= :thunk (describe-entity m)))
(defn fact? [m]
  (= :fact (describe-entity m)))
(defn literal? [m]
  (= :literal/text (describe-entity m)))

(def default-value-key
  {:thunk :thunk/label
   :literal/text :value/string})

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
