(ns kanopi.util.core
  (:require [cognitect.transit :as transit]
            [clojure.string])
  (:import java.util.UUID
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn select-with-merge
  "Selects subset of a larger configuration map, merging in the selected keys.
   Selection can be a vector, merge-keys must be a vector. In a conflict, the
   merge-keys take precedence."
  [config selection merge-keys]
  (let [get-or-in (fn [m k]
                    (if (coll? k)
                      (get-in m k)
                      (get m k)))
        subset    (get-or-in config selection)]
    (reduce (fn [m k]
              (assoc m k (get-or-in config k)))
            subset
            merge-keys)))

(defn random-uuid []
  (java.util.UUID/randomUUID))

;; ### HTTP context map helper fns

(defn get-authenticator [ctx]
  (get-in ctx [:request :authenticator]))

(defn get-data-service [ctx]
  (get-in ctx [:request :data-service]))

;; ### Datomic EntityMap helper fns for navigating the schema
;; TODO: refactor to support values of any type
(defn get-literal-or-label [ent k]
  (or (-> ent (get k) (first) (get :thunk/label))
      (-> ent (get k) (first) (get :literal/text))))

(defn fact-entity->tuple [ent]
  (let [attr (get-literal-or-label ent :fact/attribute)
        valu (get-literal-or-label ent :fact/value)]
    (vector attr valu)))

(defn transit-write [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toString out)))

(defn transit-read [stream]
  (let [string (slurp stream)]
    (if (or (nil? string) (clojure.string/blank? string))
      {}
      (let [in (ByteArrayInputStream. (.getBytes string))
            reader (transit/reader in :json)]
        (transit/read reader)))))
