(ns kanopi.csv-import
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [datomic.api :as d]
             ))

(defn load-tsv [path]
  (let [[header-row & value-rows]
        (-> path
            (io/resource)
            (io/reader)
            (csv/read-csv :separator \tab))]
    (hash-map :header-row header-row
              :value-rows value-rows)))

(defn header-item->attribute [role header-item]
  (let [attr-thunk-id (d/tempid :db.part/structure)]
    (vector
     [:db/add attr-thunk-id :thunk/label header-item]
     [:db/add attr-thunk-id :thunk/role  role])))

(defn value-item->value [value]
  (let [value-id (d/tempid :db.part/structure)]
    (vector :db/add value-id :value/string value)))

(defn attr-value->fact [attr value]
  (let [fact-id (d/tempid :db.part/structure)
        [value-id _ _ _ :as value-datom] (value-item->value value)]
    (vector
     [:db/add fact-id :fact/attribute attr]
     [:db/add fact-id :fact/value     value-id]
     value-datom)))

(defn value-row->facts [role attributes value-row]
  (-> (mapcat attr-value->fact attributes value-row)
      (vec)))

(defn csv->kanopi [path role-id]
  (let [raw-data (load-tsv path)

        attributes
        (reduce (fn [{:keys [tempids datoms] :as acc} header-item]
                  (let [new-datoms (header-item->attribute role-id header-item)]
                    (hash-map :tempids (conj tempids (ffirst new-datoms))
                              :datoms  (concat datoms new-datoms))))
                {:tempids []
                 :datoms  []}
                (:header-row raw-data))
        ]
    (->> (:value-rows raw-data)
         (mapcat (partial value-row->facts role-id (:tempids attributes)))
         (concat))))
