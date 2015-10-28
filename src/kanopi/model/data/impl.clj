(ns kanopi.model.data.impl
  (:require [datomic.api :as d]
            [kanopi.util.core :as util]
            [kanopi.model.schema :as schema]
            [kanopi.model.storage.datomic :as datomic]))

(declare describe-value-literal)

(defn entity->input [ent]
  (case (schema/describe-entity ent)
    :datum
    (vector :db/id (get ent :db/id))

    :literal
    (let [k (-> ent (dissoc :db/id) (keys) (first))]
      (vector k (get ent k)))

    ;; default
    nil))

(defn user-default-team [creds]
  (let [username (get creds :username)]
    (->> creds
         :teams
         (filter (fn [rl]
                   (= (:username creds) (:team/id rl))))
         (first)
         :db/id)))

(defn entity-id-tuple?
  "(second input) => either an integer (id) or a string (label of new datum)"
  [input]
  (and (vector? input)
       (= :db/id (first input))
       (or (integer? (second input))
           (describe-value-literal (second input)))))

(def literal-types
  {:literal/text
   {:ident :literal/text
    :predicate string?}

   :literal/integer
   {:ident :literal/integer
    :predicate integer?}

   :literal/decimal
   {:ident :literal/decimal
    :predicate (fn [v] (instance? java.lang.Double v))
    }

   ;; TODO: implement.
   :literal/uri
   {:ident :literal/uri
    :predicate (constantly nil)}

   ;; TODO: implement.
   :literal/email-address
   {:ident :literal/email-address
    :predicate (constantly nil)
    }
   })

(defn valid-tagged-literal? [literal]
  (when (vector? literal)
    (let [[tp v] literal]
      (and
       (identity tp)
       (identity v)
       (get literal-types tp)
       ((get-in literal-types [tp :predicate]) v)))))

(defn describe-value-literal [value]
  (cond
   (string? value)
   [:literal/text value]
   
   (valid-tagged-literal? value)
   value
   
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

(declare make-datomic-kv-consistent)

(defn- to-regular-map [entity-map]
  (->> entity-map
       (into {})
       (mapcat make-datomic-kv-consistent)
       (apply hash-map)))

(defn- make-datomic-kv-consistent [[k v]]
  (let [v' (cond
            (instance? datomic.query.EntityMap v)
            (list (to-regular-map v))

            (some (partial instance? datomic.query.EntityMap) v)
            (map (fn [itm]
                   (if (instance? datomic.query.EntityMap itm)
                     (to-regular-map itm)
                     itm))
                 v)

            (not (set? v))
            (list v)
            )
        ;; ensure it's a set
        v'' (if (set? v') v' (set v'))]
    [k v'']))

(defn get-fact*
  ""
  [db fact-id]
  (let [ent (d/pull db
                    '[:db/id
                      {:fact/attribute [*]}
                      {:fact/value [*]}]
                    fact-id)]
    (if (empty? (dissoc ent :db/id))
      nil
      ent)))

(defn get-datum*
  "TODO: may want to use pull api to grab facts as well.
  NOTE: ent-id can also be an ident or datomic lookup-ref."
  [db ent-id]
  (let [ent (d/pull db
                    '[:db/id
                      :datum/label
                      {:datum/team [:db/id :team/id]}
                      {:datum/fact [:db/id
                                    {:fact/attribute [*]}
                                    {:fact/value     [*]}
                                    ]}]
                    ent-id)]
    (if (empty? (dissoc ent :db/id))
      nil
      ent)))

(defn mk-literal
  ""
  ([datomic-peer creds value]
   (let [value-id (d/tempid :db.part/structure)
         ;; FIXME: this breaks with tagged value literals.
         [value-attribute parsed-value] (describe-value-literal value)
         ]
     (hash-map
      :ent-id value-id
      :txdata [[:db/add value-id value-attribute parsed-value]]))))

(defn mk-fact
  [datomic-peer creds attribute-id value-id]
  (let [fact-id (d/tempid :db.part/structure)]
    (hash-map
     :ent-id fact-id
     :txdata  [[:db/add fact-id :fact/attribute attribute-id]
               [:db/add fact-id :fact/value     value-id]])))

(defn mk-datum
  "ex: (mk-datum dp creds label [attr-id-tuple val-id-tuple]
  [attr-literal  val-id-tuple] ... etc)"
  ([datomic-peer creds]
   (mk-datum datomic-peer creds ""))

  ([datomic-peer creds label]
   (let [datum-id (d/tempid :db.part/structure)]
     (hash-map
      :ent-id  datum-id
      :txdata  [[:db/add datum-id :datum/team (user-default-team creds)]
                [:db/add datum-id :datum/label label]])))

  ([datomic-peer creds label & facts]
   ;; NOTE: switching between maps with :ent-id and :ent-ids is
   ;; confusing. should I name this structure and use a record with
   ;; some protocols to make this clearer and factor out a lot of
   ;; implementation details with composing them???
   (let [datum-excl-facts (mk-datum datomic-peer creds label)
         datum-id  (get datum-excl-facts :ent-id)

         facts     (map (partial apply add-fact->txdata datomic-peer creds datum-id) facts)
         fact-coll (reduce
                    (fn [{:keys [ent-ids txdata] :as acc} fact]
                      (hash-map :ent-ids (conj ent-ids (:ent-id fact))
                                :txdata (concat txdata (:txdata fact))))
                    {:ent-ids []
                     :txdata  []}
                    facts)]
     (hash-map
      :ent-id datum-id
      :txdata (concat [[:db/add datum-id :datum/team (user-default-team creds)]
                       [:db/add datum-id :datum/label label]]
                      (mapv (partial vector :db/add datum-id :datum/fact)
                            (get fact-coll :ent-ids))
                      (get fact-coll :txdata))))))

(defn update-datum->txdata
  [datomic-peer creds datum']
  (let [datum-id (-> datum' (get :db/id))]
    (hash-map
     :ent-id datum-id
     :txdata [
              ])))

(defmethod add-fact->txdata [::entity-id ::entity-id]
  [datomic-peer creds ent-id [_ attribute-id] [_ value-id]]
  (let [fact (mk-fact datomic-peer creds attribute-id value-id)]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (conj (get fact :txdata)
                   [:db/add ent-id :datum/fact (get fact :ent-id)]))))

(defmethod add-fact->txdata [::literal ::entity-id]
  [datomic-peer creds ent-id attribute [_ value-id]]
  (let [attr (mk-literal datomic-peer creds attribute)
        fact (mk-fact datomic-peer creds (get :ent-id attr) value-id)]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get attr :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :datum/fact (get fact :ent-id)]]))))

(defmethod add-fact->txdata [::entity-id ::literal]
  [datomic-peer creds ent-id [_ attribute-id] value]
  (let [value (mk-literal datomic-peer creds value)
        fact  (mk-fact datomic-peer creds attribute-id (get value :ent-id))]
    (hash-map
     :ent-id (get fact :ent-id)
     :txdata (concat (get value :txdata)
                     (get fact :txdata)
                     [[:db/add ent-id :datum/fact (get fact :ent-id)]]))))

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
                     [[:db/add ent-id :datum/fact (get fact :ent-id)]]))))

(defn update-fact-part->txdata
  [datomic-peer creds fact-id part input]
  (case (describe-input datomic-peer creds input)
    ::entity-id
    (let [[_ input-ent-arg] input
          input-ent
          (if (d/entity (datomic/db datomic-peer creds) input-ent-arg)
            (hash-map :ent-ids [input-ent-arg], :txdata [])
            (mk-datum datomic-peer creds input-ent-arg))]
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
    (let [fact (get-fact* (datomic/db datomic-peer creds) fact-id)
          part-ent-id (-> fact part :db/id)]
      (hash-map
       :ent-id fact-id
       :txdata (vector [:db/retract fact-id part part-ent-id])))))

(defn update-fact->txdata
  [datomic-peer creds fact-id attribute value]
  (let [fact (get-datum* (datomic/db datomic-peer creds) fact-id)]
    (hash-map
     :ent-id fact-id
     :txdata (->> [[:fact/attribute attribute]
                   [:fact/value value]]
                  (map (partial apply update-fact-part->txdata datomic-peer creds fact-id))
                  (mapcat :txdata)
                  (vec)))))

