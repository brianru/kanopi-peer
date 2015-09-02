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
