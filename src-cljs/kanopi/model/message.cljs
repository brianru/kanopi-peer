(ns kanopi.model.message
  (:require [om.core :as om]
            [cljs.core.async :as async]))

(defn- publisher [owner]
  (om/get-shared owner [:ether :publisher]))

(defn request-recent-thunks! [owner]
  (async/put! (publisher owner)
              {:noun {}
               :verb :recent-thunks
               :context {}})
  nil)

(defn request-thunk! [owner ent-id]
  (async/put! (publisher owner)
              {:noun {:ent-id ent-id}
               :verb :get
               :context {}})
  nil)

(defn toggle-fact-mode! [owner fact-id]
  (async/put! (publisher owner)
              {:noun [:fact fact-id]
               :verb :toggle-mode
               :context {}})
  nil)

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

(defn submit-statement [owner stmt]
  (async/put! (publisher owner)
              {:noun stmt
               :verb :submit-statement
               :context {}}))

(defn search [q]
  (hash-map
   :noun {:query q}
   :verb :search
   :context {}))

(defn send!
  "Ex: (->> (msg/search \"foo\") (msg/send! owner))
  "
  [owner msg]
  (async/put! (publisher owner) msg)
  nil)
