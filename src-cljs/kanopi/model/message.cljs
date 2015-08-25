(ns kanopi.model.message
  (:require [om.core :as om]
            [cljs.core.async :as async]))

(defn- publisher [owner]
  (om/get-shared owner [:ether :publisher]))

(defn request-recent-thunks! [owner]
  (async/put! (publisher owner)
              {:noun {}
               :verb :recent-thunks
               :context {}}))

(defn request-thunk! [owner ent-id]
  (async/put! (publisher owner)
              {:noun {:ent-id ent-id}
               :verb :get
               :context {}}))
