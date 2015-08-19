(ns kanopi.data-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [datomic.api :as d]
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
      (println "HERE" ent-id)
      (is (not (nil? ent-id))))
    
    (testing "thunk has user's default role"
      (is (= (:role creds) (-> ent :thunk/role first :db/id))))

    (testing "retract new thunk"
      (let [report (data/retract-thunk (:data-service system) creds ent-id) ]
        (is (nil? (data/get-thunk (:data-service system) creds ent-id)))))
    
    (component/stop system)))

;;(deftest construct-thunk
;;  (let [ creds nil
;;        [ent _] (data/init-thunk (:database system*) creds)]
;;
;;    (testing "assert fact")
;;    (testing "assert facts (single transaction)")
;;    (testing "retract fact(s)")
;;    (testing "retract thunk")
;;    (testing "retrieve at points in time")
;;    ))

;;(deftest authorization-controls
;;  (let [creds-a nil
;;        creds-b nil
;;        creds-c nil]
;;    ))

;; create all sorts of properties
;;(defspec hammer-thunk
;;  (let []
;;    (testing "assertions")
;;    (testing "retractions")
;;    ))
