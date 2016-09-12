(ns kanopi.controller.handlers.request.search
  "In-memory cache search functions, primarily used by the front-end.
  NOTE: does this belong under `response'?"
  (:require [clojure.string :as str]
            [kanopi.model.schema :as schema]))


(defn fuzzy-search-entity [q ent]
  (when (every? not-empty [q ent])
    (let [base-string (-> ent
                          (schema/get-value)
                          (str)
                          (or ""))
          query-string (str/lower-case q)
          match-string (re-find (re-pattern query-string) base-string)]
      (when-not (or (str/blank? base-string)
                    (str/blank? match-string))
        (list (/ (count base-string) (count match-string))
              ent)))))

; TODO: refactor "entity-type" to "input-type" and use
; schema/get-input-type to pull that from each entity
(defn matching-entity-type [tp ent]
  (if-not tp true
    (= tp (schema/describe-entity ent))))

(defn local-fulltext-search
  "TODO: sort by match quality
  https://github.com/Yomguithereal/clj-fuzzy
  TODO: handle upper- vs lower-case better
  TODO: only show x many
  TODO: deal with empty q better
  "
  [app-state q tp]
  (let []
    (->> (get-in app-state [:cache])
         (vals)
         ;(filter (partial matching-entity-type tp))
         (map    (partial fuzzy-search-entity q))
         (remove nil?)
         (distinct)
         (sort-by first)
         (vec))))
