(ns kanopi.model.ref-cursors
  "Ref cursor helper functions."
  (:require [om.core :as om]))

(defn get-ref-cursor [owner k]
  (let [constructor-fn (om/get-shared owner k)]
    (constructor-fn)))

(defn mk-ref-cursor-fn [app-state k]
  (fn []
    (-> app-state
        (om/root-cursor)
        (get k)
        (om/ref-cursor))))

(defn mk-ref-cursor-map [app-state ks]
  (->> ks
       (map (partial mk-ref-cursor-fn app-state))
       (zipmap ks)))
