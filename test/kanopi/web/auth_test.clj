(ns kanopi.web.auth-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :as component]
            [cemerick.friend.credentials :as creds]
            [datomic.api :as d]
            [kanopi.data :as data]
            [kanopi.test-util :as test-util]
            [kanopi.web.auth :refer :all]))

(deftest register
  (let [sys (-> (test-util/system-excl-web)
                (component/start))
        username "brian"
        password "rubinton"
        res   (register! (:authenticator sys) username password)
        creds (credentials (:authenticator sys) username)]

    (testing "valid credentials"
      (clojure.pprint/pprint creds)
      (is (valid-credentials? creds)))

    (testing "transaction succeeded"
      (is (not (nil? res)))
      (is (= username (get creds :username))))

    ;; FIXME: don't understand why this is failing
    (testing "user stored as datum"
      (let [user (data/get-datum (:data-service sys) creds (get creds :ent-id))]
        (is (not-empty user))))

    ;; FIXME: don't understand why this is failing
    (testing "user role created"
      (let [role (data/get-datum (:data-service sys) creds (-> creds :role first :db/id))]
        (is (not-empty role))))

    (testing "password crypto works"
      (is (not= password (get creds :password)))
      (is (creds/bcrypt-verify password (get creds :password))))

    ;; FIXME: failing.
    (testing "initial data loaded"
      (let [user-data (d/q '[:find [?e ...]
                             :in $ ?user-role
                             :where [?e :datum/role ?user-role]]
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
  (testing ""
    (let []
      )))

