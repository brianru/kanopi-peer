(ns kanopi.data-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [datomic.api :as d]
            [kanopi.system :as sys]
            [kanopi.data :as data]
            [kanopi.generators :refer :all]
            [kanopi.test-util :as test-util]
            [com.stuartsierra.component :as component]))

(defonce ^:dynamic *system* nil)

(use-fixtures :each test-util/system-excl-web-fixture)


(deftest init-thunk
  (let [ creds nil
        [ent _] (data/init-thunk! (:database *system*) creds)]

    (testing "init thunk return vals"
      (is (not-empty ent)))
    
    (testing "retrieve new thunk"
      (is (not-empty (data/get-thunk database creds (:db/id ent)))))

    (testing "retract new thunk"
      )
    ))

(deftest construct-thunk
  (let [ creds nil
        [ent _] (data/init-thunk! (:database *system*) creds)]

    (testing "assert fact")
    (testing "assert facts (single transaction)")
    (testing "retract fact(s)")
    (testing "retract thunk")
    (testing "retrieve at points in time")
    ))

(deftest authorization-controls
  (let [creds-a nil
        creds-b nil
        creds-c nil]
    ))

;; create all sorts of properties
(defspec hammer-thunk
  (let []
    (testing "assertions")
    (testing "retractions")
    ))
