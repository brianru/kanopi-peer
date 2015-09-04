(ns kanopi.model.message
  (:require [om.core :as om]
            [cljs.core.async :as async]))

(defn- publisher [owner]
  (om/get-shared owner [:ether :publisher]))

(defn send!
  "Ex: (->> (msg/search \"foo\") (msg/send! owner))
  "
  [owner msg]
  (async/put! (publisher owner) msg)
  ;; NOTE: js evt handlers don't like `false` as a return value, which
  ;; async/put! often returns. So we add a nil.
  nil)

(defn toggle-fact-mode [ent]
  (hash-map
   :noun [:fact (:db/id ent)]
   :verb :toggle-mode
   :context {}))

(defn update-fact-part [ent v]
  (hash-map
   :noun nil))

(defn change-entity-type [ent desired-type]
  (hash-map
   :noun {:existing-entity ent
          :desired-type desired-type}
   :verb :change-entity-type
   :context {}))

(defn update-entity-value [ent new-value]
  (hash-map
   :noun {:existing-entity ent
          :new-value new-value}
   :verb :update-entity-value
   :context {}))

(defn search [q]
  (hash-map
   :noun {:query q}
   :verb :search
   :context {}))

