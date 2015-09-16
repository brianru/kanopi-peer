(ns kanopi.model.message
  (:require [om.core :as om]
            [cljs.core.async :as async]))

(defn publisher [owner]
  (om/get-shared owner [:ether :publisher]))

(defn send!
  "Ex: (->> (msg/search \"foo\") (msg/send! owner))
  TODO: allow specification of transducers or debounce msec via args
        - otherwise user must create some extra wiring on their end,
          which is what this fn is trying to avoid. layered abstractions."
  ([owner msg & args]
   (async/put! (publisher owner) msg)
   ;; NOTE: js evt handlers don't like `false` as a return value, which
   ;; async/put! often returns. So we add a nil.
   nil
   ))

;; local component message
(defn toggle-fact-mode [ent]
  (hash-map
   :noun [:fact (:db/id ent)]
   :verb :toggle-mode
   :context {}))

;; local component message
(defn select-fact-part-type [fact-id fact-part tp]
  (hash-map
   :noun [:fact fact-id]
   :verb :select-fact-part-type
   :context {:fact-part fact-part
             :value tp}))

;; local component message
(defn input-fact-part-value [fact-id fact-part input-value]
  (hash-map
   :noun [:fact fact-id]
   :verb :input-fact-part-value
   :context {:fact-part fact-part
             :value input-value}))

;; local component message
(defn select-fact-part-reference [fact-id fact-part selection]
  (hash-map
   :noun [:fact fact-id]
   :verb :select-fact-part-reference
   :context {:fact-part fact-part
             :selection selection}))

(defn update-fact [thunk-id fact]
  (hash-map
   :noun {:thunk-id thunk-id
          :fact fact}
   :verb :update-fact
   :context {}))

(defn update-thunk-label [ent new-label]
  (hash-map
   :noun {:existing-entity ent
          :new-label new-label}
   :verb :update-thunk-label
   :context {}))

(defn search
  ([q]
   (search q nil))
  ([q tp]
   (hash-map
    :noun {:query-string q
           :entity-type  tp}
    :verb :search
    :context {})))

