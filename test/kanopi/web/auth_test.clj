(ns kanopi.web.auth-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :as component]
            [cemerick.friend.credentials :as creds]
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

    (testing "transaction succeeded"
      (is (not (nil? res)))
      (is (= username (:username creds))))

    (testing "user stored as datum"
      (is (data/get-datum (:data-service sys) creds (:ent-id creds))))

    (testing "user role created"
      (is (data/get-datum (:data-service sys) creds (:role creds))))

    (testing "password crypto works"
      (is (not= password (:password creds)))
      (is (creds/bcrypt-verify password (:password creds))))

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

