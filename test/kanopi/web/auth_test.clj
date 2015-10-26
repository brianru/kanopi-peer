(ns kanopi.web.auth-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :as component]
            [cemerick.friend.credentials :as creds]
            [datomic.api :as d]
            [schema.core :as s]
            [kanopi.model.data :as data]
            [kanopi.test-util :as test-util]
            [kanopi.model.schema :as schema]
            [kanopi.web.auth :refer :all]))

(deftest register
  (let [sys (-> (test-util/system-excl-web)
                (component/start))
        username "brian"
        password "rubinton"
        res      (register!   (:authenticator sys) username password)
        creds    (credentials (:authenticator sys) username)]

    (testing "schema"
      (is (s/validate schema/Credentials creds)))

    (testing "transaction succeeded"
      (is (= username (get creds :username))))

    (testing "user stored as datum"
      (let [user (d/entity (d/db (get-in sys [:datomic-peer :connection]))
                           (get creds :ent-id))]
        (is (not-empty user))))

    (testing "user role created"
      (let [role (d/entity (d/db (get-in sys [:datomic-peer :connection]))
                           (-> creds :role first :db/id))]
        (is (not-empty role))))

    (testing "password crypto works"
      (is (not= password (get creds :password)))
      (is (creds/bcrypt-verify password (get creds :password))))

    (testing "initial data loaded"
      (let [user-data (d/q '[:find [?e ...]
                             :in $ ?user-role
                             :where
                             [?e :datum/role ?user-role]]
                           (d/db (get-in sys [:datomic-peer :connection]))
                           (-> creds :role first :db/id))]
        (is (not-empty user-data))))

    (component/stop sys)))

(deftest register-existing-username
  (let [sys (-> (test-util/system-excl-web)
                (component/start))
        username "brian"
        password "rubinton"
        report (register! (:authenticator sys) username password)]

    (testing "throw exception when registering an existing username"
      (is (thrown? java.lang.AssertionError
                   (register! (:authenticator sys) username password))))

    (testing "return something when registering a slightly different username"
      (is (not-empty
           (do (register! (:authenticator sys) (str username "blah") password)
               (credentials (:authenticator sys) (str username "blah"))))))

    (component/stop sys)))

(deftest change-password
  (let [sys (-> (test-util/system-excl-web)
                (component/start))
        username  "brian"
        password  "rubinton"
        password' "oreos123"
        _         (register! (:authenticator sys) username password)]
    (testing "before"
      (is (verify-creds (:authenticator sys) username password)))
    (testing "change password"
      (is (change-password! (:authenticator sys) username password password')))
    (testing "after"
      (is (verify-creds (:authenticator sys) username password')))

    (component/stop sys)))
