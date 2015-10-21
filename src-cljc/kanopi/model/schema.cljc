(ns kanopi.model.schema)

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
