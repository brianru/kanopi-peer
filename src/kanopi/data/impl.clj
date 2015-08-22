(ns kanopi.data.impl
  (:require [datomic.api :as d]
            [kanopi.util.core :as util]
            [kanopi.storage.datomic :as datomic]))

(declare describe-value-literal)

(defn entity-id-tuple?
  "(second input) => either an integer (id) or a string (label of new thunk)"
  [input]
  (and (vector? input)
       (= :db/id (first input))
       (or (integer? (second input))
           (describe-value-literal (second input)))))

(defn describe-value-literal [value]
  (cond
   (string? value)
   :value/string

   :default
   nil))

(defn value-literal? [input]
  (describe-value-literal input))

(defn describe-input [datomic-peer creds input]
  (cond
   (entity-id-tuple? input)
   ::entity-id

   (value-literal? input)
   ::literal
   
   :default
   ::retract))

;; TODO: Is it simpler to separate this into add-fact-attribute and
;; add-fact-value helper fns? This way there'd be 2 functions with 2
;; cases each, instead of 4 functions representing each of the 4
;; cases. Seems like less overlap, but another layer of indirection.
(defmulti add-fact->txdata
  "Generate transaction data for asserting the given [eid attr value].

  Consumers will pass either literals of entity refs to attr and value. The methods should take those inputs and generate the necessary transaction data (including nested entities, as necessary).

  NOTE: at this stage we're only working with references and strings."
  (fn [datomic-peer creds ent-id attribute value]
    (mapv (partial describe-input datomic-peer creds)
          [attribute value])))

(defn retract-entity->txdata
  "Generate transaction data for retracting the specified entity."
  [datomic-peer creds ent-id]
  (hash-map
   :ent-id nil
   :txdata [[:db.fn/retractEntity ent-id]]))

(defn get-entity*
  "TODO: may want to use pull api to grab facts as well.
  NOTE: ent-id can also be an ident or datomic lookup-ref."
  [db ent-id]
  (when-let [ent (d/touch (not-empty (d/entity db ent-id)))]
    (->> ent
         (mapcat (fn [[k v]]
                   (let [v' (cond
                             (instance? datomic.query.EntityMap v)
                             (list v)

                             (every? (partial instance? datomic.query.EntityMap) v)
                             v

                             (not (set? v))
                             (list v)
                             )
                         ;; ensure it's a set
                         v'' (if (set? v') v' (set v'))]
                     [k v''])))
         (apply hash-map))))

(defn mk-literal
  ""
  ([datomic-peer creds value]
   (let [value-id (d/tempid :db.part/structure)
         value-attribute (describe-value-literal value)
         ]
     (hash-map
      :ent-id value-id
      :txdata [[:db/add value-id value-attribute value]]))))

(defn mk-fact
  [datomic-peer creds attribute-id value-id]
  (let [fact-id (d/tempid :db.part/structure)]
    (hash-map
     :ent-id fact-id
     :txdata  [[:db/add fact-id :fact/attribute attribute-id]
               [:db/add fact-id :fact/value     value-id]])))

(defn mk-thunk
  "ex: (mk-thunk dp creds label [attr-id-tuple val-id-tuple]
  [attr-literal  val-id-tuple] ... etc)"
  ([datomic-peer creds]
   (mk-thunk datomic-peer creds ""))

  ([datomic-peer creds label]
   (let [thunk-id (d/tempid :db.part/structure)]
     (hash-map
      :ent-id thunk-id
      :txdata [[:db/add thunk-id :thunk/role (get creds :role)]
               [:db/add thunk-id :thunk/label label]])))

  ([datomic-peer creds label & facts]
   (let [thunk-id  (d/tempid :db.part/structure)
         facts     (map (partial apply add-fact->txdata datomic-peer creds thunk-id) facts)
         fact-coll (reduce
                    (fn [{:keys [ent-ids txdata] :as acc} fact]
                      (hash-map :ent-ids (conj ent-ids (:ent-id fact))
                                :txdata (concat txdata (:txdata fact))))
                    {:ent-ids []
                     :txdata  []}
                    facts)]
     (hash-map
      :ent-id thunk-id
      :txdata (concat [[:db/add thunk-id :thunk/role  (get creds :role)]
                       [:db/add thunk-id :thunk/label label]]
                      (mapv (partial vector :db/add thunk-id :thunk/fact)
                            (get fact-coll :ent-ids))
                      (get fact-coll :txdata))))))

(defmethod add-fact->txdata [::entity-id ::entity-id]
  [datomic-peer creds ent-id [_ attribute-id] [_ value-id]]
  (let [fact (mk-fact datomic-peer creds attribute-id value-id)]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (conj (get fact :txdata)
                   [:db/add ent-id :thunk/fact (get fact :ent-id)]))))

(defmethod add-fact->txdata [::literal ::entity-id]
  [datomic-peer creds ent-id attribute [_ value-id]]
  (let [attr (mk-literal datomic-peer creds attribute)
        fact (mk-fact datomic-peer creds (get :ent-id attr) value-id)]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get attr :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :thunk/fact (get fact :ent-id)]]))))

(defmethod add-fact->txdata [::entity-id ::literal]
  [datomic-peer creds ent-id [_ attribute-id] value]
  (let [value (mk-literal datomic-peer creds value)
        fact  (mk-fact datomic-peer creds attribute-id (get value :ent-id))]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get value :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :thunk/fact (get fact :ent-id)]]))))

(defmethod add-fact->txdata [::literal ::literal]
  [datomic-peer creds ent-id attribute value]
  (let [attr  (mk-literal datomic-peer creds attribute)
        value (mk-literal datomic-peer creds value)
        fact  (mk-fact datomic-peer creds (get attr :ent-id) (get value :ent-id))]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get value :txdata)
                     (get attr :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :thunk/fact (get fact :ent-id)]]))))

(defn update-fact-part->txdata
  [datomic-peer creds fact-id part input]
  (case (describe-input datomic-peer creds input)
     ::entity-id
     (let [[_ input-ent-arg] input
           input-ent
           (if (d/entity (datomic/db datomic-peer creds) input-ent-arg)
             (hash-map :ent-id input-ent-arg, :txdata [])
             (mk-thunk datomic-peer creds input-ent-arg))]
       (println "update-fact-part->txdata" input-ent)
       (hash-map
        :ent-id fact-id
        :txdata (conj (get input-ent :txdata)
                      [:db/add fact-id part (get input-ent :ent-id)])))

     ::literal
     (let [literal (mk-literal datomic-peer creds input)]
       (hash-map
        :ent-id fact-id
        :txdata (conj (get literal :txdata)
                      [:db/add fact-id part (get literal :ent-id)])))

     ::retract
     (let [fact (get-entity* (datomic/db datomic-peer creds) fact-id)
           part-ent-id (-> fact part first :db/id)]
       (hash-map
        :ent-id fact-id
        :txdata (vector [:db/retract fact-id part part-ent-id])))))

(defn update-fact->txdata
  [datomic-peer creds fact-id attribute value]
  (let [fact (get-entity* (datomic/db datomic-peer creds) fact-id)]
    (hash-map
     :ent-id fact-id
     :txdata (->> [[:fact/attribute attribute]
                   [:fact/value value]]
                  (map (partial apply update-fact-part->txdata datomic-peer creds fact-id))
                  (mapcat :txdata)
                  (vec)))))

