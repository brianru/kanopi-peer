(ns kanopi.data-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.set]
            [datomic.api :as d]
            [kanopi.util.core :as util]
            [kanopi.system :as sys]
            [kanopi.data :as data]
            [kanopi.storage.datomic :as datomic]
            [kanopi.web.auth :as auth]
            [kanopi.generators :refer :all]
            [kanopi.test-util :as test-util]
            [com.stuartsierra.component :as component]))

(deftest init-thunk
  (let [system (component/start (test-util/system-excl-web))

        creds  (do (auth/register! (:authenticator system) "brian" "rubinton")
                   (auth/credentials (:authenticator system) "brian"))
        ent-id (data/init-thunk (:data-service system) creds)
        ent    (data/get-thunk (:data-service system) creds ent-id)]

    (testing "init thunk returns the thunk's entity id"
      (is (not (nil? ent-id))))
    
    (testing "thunk has user's default role"
      (is (= (:role creds) (-> ent :thunk/role first :db/id))))

    (testing "thunk shape as given by data service"
      (is (every? keyword? (keys ent)))
      (is (->> (get ent :thunk/role)
               (set?)))
      (is (->> (get ent :thunk/role)
               (every? (partial instance? datomic.query.EntityMap))))
      (is (->> (get ent :thunk/role)
               (map :db/id)
               (every? integer?)))
      (is (->> (get ent :thunk/label)
               (every? string?)))
      (when (get ent :thunk/fact)
        (is (->> (get ent :thunk/fact)
                 (set?)))
        (is (->> (get ent :thunk/fact)
                 (every? (partial instance? datomic.query.EntityMap))))
        (is (->> (get ent :thunk/fact)
                 (map :db/id)
                 (every? integer?)))
        ))

    (testing "retract new thunk"
      (let [report (data/retract-thunk (:data-service system) creds ent-id) ]
        (is (nil? (data/get-thunk (:data-service system) creds ent-id)))))

    (component/stop system)))

(deftest construct-thunk
  (let [system (component/start (test-util/system-excl-web))
        creds  (do (auth/register! (:authenticator system) "brian" "rubinton")
                   (auth/credentials (:authenticator system) "brian"))

        ent-id (data/init-thunk (:data-service system) creds)
        ent-0  (data/get-thunk (:data-service system) creds ent-id)
        fact-1 ["age" "42"]
        ent-1  (apply data/add-fact (:data-service system) creds ent-id fact-1)
        fact-id (-> ent-1 :thunk/fact first :db/id)
        ]

    (testing "assert fact"
      (let [new-fact (->> (clojure.set/difference (get ent-1 :thunk/fact)
                                                  (get ent-0 :thunk/fact))
                          (first)
                          (util/fact-entity->tuple))]
        (is (= new-fact fact-1))))

    (testing "update-fact: change value literal. attr label same."
      (let [fact' ["age" "new-value!"]
            ent' (apply data/update-fact (:data-service system) creds fact-id fact')
            fact-ent (-> ent' :thunk/fact first)]
        (is (= "age" (-> fact-ent :fact/attribute :thunk/label)))
        (is (nil? (-> fact-ent :fact/value :value/ref)))
        (is (not (nil? (-> fact-ent :fact/value :value/string))))
        (is (= "new-value!" (-> fact-ent :fact/value :value/ref :thunk/label)))))

    (testing "update-fact: change value literal. attr label nil."
      (let [fact' [nil "new-value2!"]
            ent' (apply data/update-fact (:data-service system) creds fact-id fact')
            fact-ent (-> ent' :thunk/fact first)]
        (is (= "age" (-> fact-ent :fact/attribute :thunk/label)))
        (is (nil? (-> fact-ent :fact/value :value/ref)))
        (is (not (nil? (-> fact-ent :fact/value :value/string))))
        (is (= "new-value2!" (-> fact-ent :fact/value :value/ref :thunk/label)))))

    (testing "update-fact: change value from literal to ref"
      (let [fact' ["age2" [:db/id "42"]]
            ent'  (apply data/update-fact (:data-service system) creds fact-id fact')
            fact-ent (-> ent' :thunk/fact first)]
        (is (= "age2" (-> fact-ent :fact/attribute :thunk/label)))
        (is (nil? (-> fact-ent :fact/value :value/string)))
        (is (not (nil? (-> fact-ent :fact/value :value/ref))))
        (is (= "42" (-> fact-ent :fact/value :value/ref :thunk/label)))))

    (testing "update-fact: change value from ref to literal"
      (let [fact' ["age2" "43"]
            ent'  (apply data/update-fact (:data-service system) creds fact-id fact')
            fact-ent (-> ent' :thunk/fact first)]
        (is (= "age2" (-> fact-ent :fact/attribute :thunk/label)))
        (is (nil? (-> fact-ent :fact/value :value/ref)))
        (is (not (nil? (-> fact-ent :fact/value :value/string))))
        (is (= "43" (-> fact-ent :fact/value :value/string)))))

    (testing "update-fact: change attribute literal"
      )

    (testing "update-fact: change attribute from literal to ref"
      )

    (testing "update-fact: change attribute from ref to literal"
      )

    (testing "retract fact"
      )))

;;(deftest authorization-controls
;;  (let [creds-a nil
;;        creds-b nil
;;        creds-c nil]
;;    ))
