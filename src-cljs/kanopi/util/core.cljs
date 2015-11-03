(ns kanopi.util.core
  (:require [cljs-uuid-utils.core :as uuid]
            cljs.reader))

(defn random-uuid []
  (str (uuid/make-random-uuid)))

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

;; Should this go somewhere more prominent?
(def last-id (atom -111111))

(defn next-id []
  (swap! last-id dec))

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

(defn try-read-string [string]
  (try
   (cljs.reader/read-string string)
   (catch js/Object e
     string)))
