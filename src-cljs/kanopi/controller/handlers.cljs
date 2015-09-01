(ns kanopi.controller.handlers
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]))

(defn- lookup-id
  ([props id]
   (->> (get-in props [:cache id])
       (reduce (fn [acc [k v]]
                 (cond
                  (= k :thunk/fact)
                  (assoc acc k (set (map (partial lookup-id props) v)))
                  (= k :fact/attribute)
                  (assoc acc k (set (map (partial lookup-id props) v)))
                  (= k :fact/value)
                  (assoc acc k (set (map (partial lookup-id props) v)))

                  :default
                  (assoc acc k v)))
               {})
       ))
  )

(defn- build-thunk-data
  "
  Data is stored as flat maps locally and on server, but to simplify
  thunk component model we must nest entities as follows:
  thunk -> facts -> attributes (literals or thunks)
                 -> values     (literals or thunks)"
  [props thunk-id]
  {:pre [(integer? thunk-id)]}
  (let [thunk (lookup-id props thunk-id)]
    (hash-map
     :context-thunks nil
     :thunk thunk
     :similar-thunks nil)))

(defn- navigate-to-thunk [props msg]
  (let [thunk-id (cljs.reader/read-string (get-in msg [:noun :route-params :id]))
        thunk' (build-thunk-data props thunk-id)]
    (assoc props :thunk thunk')))

(defn navigate!
  "Transform app-state to support requested page."
  [props]
  (fn [msg]
    (info "navigate:" msg)
    (om/transact! props
                  (fn [app-state]
                    (cond-> app-state
                      true
                      (assoc :page (get msg :noun))

                      (= :thunk (get-in msg [:noun :handler]))
                      (navigate-to-thunk msg)

                      (not= :thunk (get-in msg [:noun :handler]))
                      (assoc :thunk {})

                      )))))
