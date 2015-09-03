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
   :unknown))

(defn thunk? [m]
  (= :thunk (describe-entity m)))
(defn fact? [m]
  (= :fact (describe-entity m)))
(defn literal? [m]
  (= :literal (describe-entity m)))

(defn get-value
  ([m]
   (get-value m ""))
  ([m default-value]
   (case (describe-entity m)
     :thunk
     (:thunk/label m)

     :literal
     (:value/string m)

     default-value
     )))

(defn display-entity [m]
 (get-value m "help, I'm trapped!"))
