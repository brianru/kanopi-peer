(ns kanopi.model.schema)

(defn describe-entity [m]
  (cond
   (contains? m :thunk/label)
   :thunk
   (contains? m :fact/attribute)
   :fact
   (contains? m :value/string)
   :literal
   :default
   nil))

(defn thunk? [m]
  (= :thunk (describe-entity m)))
(defn fact? [m]
  (= :fact (describe-entity m)))
(defn literal? [m]
  (= :literal (describe-entity m)))
