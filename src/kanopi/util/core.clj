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

(defn deep-merge
  "Deeply merges maps so that nested maps are combined rather than replaced.
  For example:
  (deep-merge {:foo {:bar :baz}} {:foo {:fuzz :buzz}})
  ;;=> {:foo {:bar :baz, :fuzz :buzz}}
  ;; contrast with clojure.core/merge
  (merge {:foo {:bar :baz}} {:foo {:fuzz :buzz}})
  ;;=> {:foo {:fuzz :quzz}} ; note how last value for :foo wins"
  [& vs]
  (if (every? map? vs)
    (apply merge-with deep-merge vs)
    (last vs)))

(defn deep-merge-with
  "Deeply merges like `deep-merge`, but uses `f` to produce a value from the
  conflicting values for a key in multiple maps."
  [f & vs]
  (if (every? map? vs)
    (apply merge-with (partial deep-merge-with f) vs)
    (apply f vs)))

(defn random-uuid []
  (java.util.UUID/randomUUID))

;; ### HTTP context map helper fns

(defn get-authenticator [ctx]
  (get-in ctx [:request :authenticator]))

(defn get-auth-fn [ctx]
  (get-in ctx [:request :authenticator :user-lookup-fn]))

(defn get-data-service [ctx]
  (get-in ctx [:request :data-service]))

(defn get-web-handler [web-app]
  (get web-app :app-handler))

;; ### Datomic EntityMap helper fns for navigating the schema
;; TODO: refactor to support values of any type
(defn get-literal-or-label [ent k]
  (or (-> ent (get k) (get :datum/label))
      (-> ent (get k) (dissoc :db/id) (vals) (first))))

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
  ;; NOTE: type hint avoids warning when calling .getBytes below
  (let [^java.lang.String string (slurp stream)]
    (if (or (nil? string) (clojure.string/blank? string))
      {}
      (let [in (ByteArrayInputStream. (.getBytes string))
            reader (transit/reader in :json)]
        (transit/read reader)))))
