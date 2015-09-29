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
            [kanopi.data.impl :as impl]
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
      (is (= (impl/user-default-role creds) (-> ent :thunk/role first :db/id))))

    (testing "thunk shape as given by data service"
      (is (every? keyword? (keys ent)))
      (is (->> (get ent :thunk/role)
               (coll?)))
      (is (->> (get ent :thunk/role)
               (every? map?)))
      (is (->> (get ent :thunk/role)
               (map :db/id)
               (every? integer?)))
      (is (->> (get ent :thunk/label)
               (string?)))
      (is (= "banana boat" (get ent :thunk/label)))

      (when (get ent :thunk/fact)
        (is (->> (get ent :thunk/fact)
                 (coll?)))
        (is (->> (get ent :thunk/fact)
                 (every? map?)))
        (is (->> (get ent :thunk/fact)
                 (map :db/id)
                 (every? integer?)))
        ))

    (testing "retract new thunk"
      (let [report (data/retract-thunk (:data-service system) creds ent-id)]
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
      (let [new-fact (->> (clojure.set/difference (set (get ent-1 :thunk/fact))
                                                  (set (get ent-0 :thunk/fact)))
                          (first))]
        (is (= (util/fact-entity->tuple new-fact) fact-1))))

    (testing "update-fact: change value literal. attr label same."
      (let [fact' ["age" "new-value!"]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= fact' (util/fact-entity->tuple fact-ent)))))

    (testing "update-fact: change value literal. attr label nil."
      (let [fact' [nil "new-value2!"]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= fact' (util/fact-entity->tuple fact-ent)))))

    (testing "update-fact: change value from literal to ref"
      (let [fact' ["age2" [:db/id "42"]]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= ["age2" "42"] (util/fact-entity->tuple fact-ent)))))

    (testing "update-fact: change value from ref to literal"
      (let [fact' ["age2" "43"]
            fact-ent  (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= fact' (util/fact-entity->tuple fact-ent)))))

    (testing "update-fact: change attribute from literal to ref"
      (let [fact' ["age2" [:db/id "infinity"]]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= ["age2" "infinity"] (util/fact-entity->tuple fact-ent)))))

    (testing "update-fact: change attribute from ref to literal"
      (let [fact' ["age2" "a real number"]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= fact' (util/fact-entity->tuple fact-ent)))))

    (testing "retract fact"
      )))

;; TODO: test user-thunk input fns
(deftest context-thunks
  (let []
    
    ))

(deftest similar-thunks
  (let []
    ))

;;(deftest authorization-controls
;;  (let [creds-a nil
;;        creds-b nil
;;        creds-c nil]
;;    ))
