(ns kanopi.util.core
  (:require [cognitect.transit :as transit]
            clojure.string)
  (:import java.util.UUID
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn random-uuid []
  (str (java.util.UUID/randomUUID)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Sequential ID Creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def last-id (atom -111111))

(defn next-id []
  (swap! last-id dec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Fuzzy text matching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- next-row
  [previous current other-seq]
  (reduce
    (fn [row [diagonal above other]]
      (let [update-val (if (= other current)
                          diagonal
                          (inc (min diagonal above (first row))))]
        (conj row update-val)))
    [(inc (first previous))]
    (map vector previous (next previous) other-seq)))

(defn levenshtein
  "Compute the levenshtein distance between two [sequences]."
  [sequence1 sequence2]
  (first
    (reduce (fn [previous current] (next-row previous current sequence2))
            (map #(identity %2) (cons nil sequence2) (range))
            sequence1)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Map stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

(defn sort-by-ordering
  "Similar signature as sort-by.
  Lets the user supply an ordering instead of a comparator fn."
  [keyfn ordering coll]
  (let [vals->indexes (into {} (map-indexed (comp vec reverse list) ordering))
        compfn (fn [v1 v2]
                 (compare (get vals->indexes v1) (get vals->indexes v2)))]
    (sort-by keyfn compfn coll)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Ring HTTP Context map helper fns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-authenticator [ctx]
  (get-in ctx [:request :authenticator]))

(defn get-auth-fn [ctx]
  (get-in ctx [:request :authenticator :user-lookup-fn]))

(defn get-data-service [ctx]
  (get-in ctx [:request :data-service]))

(defn get-web-handler [web-app]
  (get web-app :app-handler))

(defn get-session-service [ctx]
  (get-in ctx [:request :session-service]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Ring HTTP Context map helper fns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-literal-or-label [ent k]
  (or (-> ent (get k) (get :datum/label))
      (-> ent (get k) (dissoc :db/id :literal/team) (vals) (first))))

(defn fact-entity->tuple [ent]
  (let [attr (get-literal-or-label ent :fact/attribute)
        valu (get-literal-or-label ent :fact/value)]
    (vector attr valu)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; String ID parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn try-read-string [string]
  (try
   (#?(:cljs cljs.reader/read-string
       :clj  read-string)
            string)
   (catch #?(:cljs js/Object :clj Exception) e
     string)))

(defn read-entity-id
  "Sometimes an integer, sometimes a string, never a symbol.
  Negative numbers are allowed because they represent temp ids."
  [input]
  (if (= input (and (string? input) (re-find #"-?[0-9]*" input)))
    (try-read-string input)
    input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Transit helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn transit-write [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toString out)))

(defn transit-read [stream]
  ;; NOTE: type hint avoids warning when calling .getBytes below
  (when stream
    (let [^java.lang.String string (slurp stream)]
      (if (or (nil? string) (clojure.string/blank? string))
        {}
        (let [in (ByteArrayInputStream. (.getBytes string))
              reader (transit/reader in :json)]
          (transit/read reader))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Keyword helper fns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- namespace-string [& args]
  (apply str (interpose "." (remove nil? args))))

(defn keyword-conj [kw x]
  (keyword (namespace-string (namespace kw) (name kw))
           x))
