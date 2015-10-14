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

(deftest init-datum
  (let [system (component/start (test-util/system-excl-web))

        creds  (do (auth/register! (:authenticator system) "brian" "rubinton")
                   (auth/credentials (:authenticator system) "brian"))
        ent-id (data/init-datum (:data-service system) creds)
        ent    (data/get-datum (:data-service system) creds ent-id)]

    (testing "init datum returns the datum's entity id"
      (is (not (nil? ent-id))))
    
    (testing "datum has user's default role"
      (is (= (impl/user-default-role creds) (-> ent :datum/role first :db/id))))

    (testing "datum shape as given by data service"
      (is (every? keyword? (keys ent)))
      (is (->> (get ent :datum/role)
               (coll?)))
      (is (->> (get ent :datum/role)
               (every? map?)))
      (is (->> (get ent :datum/role)
               (map :db/id)
               (every? integer?)))
      (is (->> (get ent :datum/label)
               (string?)))
      (is (= "banana boat" (get ent :datum/label)))

      (when (get ent :datum/fact)
        (is (->> (get ent :datum/fact)
                 (coll?)))
        (is (->> (get ent :datum/fact)
                 (every? map?)))
        (is (->> (get ent :datum/fact)
                 (map :db/id)
                 (every? integer?)))
        ))

    (testing "retract new datum"
      (let [report (data/retract-datum (:data-service system) creds ent-id)]
        (is (nil? (data/get-datum (:data-service system) creds ent-id)))))

    (component/stop system)))

(deftest construct-datum
  (let [system (component/start (test-util/system-excl-web))
        creds  (do (auth/register! (:authenticator system) "brian" "rubinton")
                   (auth/credentials (:authenticator system) "brian"))

        ent-id (data/init-datum (:data-service system) creds)
        ent-0  (data/get-datum (:data-service system) creds ent-id)
        fact-1 ["age" "42"]
        ent-1  (apply data/add-fact (:data-service system) creds ent-id fact-1)
        fact-id (-> ent-1 :datum/fact first :db/id)
        ]

    (testing "assert fact"
      (let [new-fact (->> (clojure.set/difference (set (get ent-1 :datum/fact))
                                                  (set (get ent-0 :datum/fact)))
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

    (testing "update datum label"
      (let [lbl' "new label dude!"
            datum' (data/update-datum-label (:data-service system) creds ent-id lbl')]
        (is (= lbl' (get datum' :datum/label)))))))

(deftest literal-types
  (let [system  (component/start (test-util/system-excl-web))
        creds   (do (auth/register!   (:authenticator system) "brian" "rubinton")
                    (auth/credentials (:authenticator system) "brian"))
        ent-id  (data/init-datum (:data-service system) creds)
        ent-0   (data/get-datum  (:data-service system) creds ent-id)
        fact-1  ["age" "42"]
        ent-1   (apply data/add-fact (:data-service system) creds ent-id fact-1)
        fact-id (-> ent-1 :datum/fact first :db/id)
        ]
    (testing "tagged text literal"
      (let [fact' ["age" [:literal/text "new-value!"]]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= ["age" "new-value!"] (util/fact-entity->tuple fact-ent)))))

    (testing "tagged integer literal"
      (let [fact' ["age" [:literal/integer 42]]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= ["age" 42] (util/fact-entity->tuple fact-ent)))))

    (testing "tagged decimal literal"
      (let [fact' ["age" [:literal/decimal 73.65]]
            fact-ent (apply data/update-fact (:data-service system) creds fact-id fact')]
        (is (= ["age" 73.65] (util/fact-entity->tuple fact-ent)))))

    (testing "tagged uri literal")
    (testing "tagged email-address literal")
    ))

;; TODO: test user-datum input fns
(deftest context-datums
  (let []
    
    ))

(deftest similar-datums
  (let []
    ))

;;(deftest authorization-controls
;;  (let [creds-a nil
;;        creds-b nil
;;        creds-c nil]
;;    ))
