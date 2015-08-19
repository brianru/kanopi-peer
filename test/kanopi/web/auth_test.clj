(ns kanopi.web.auth-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :as component]
            [cemerick.friend.credentials :as creds]
            [datomic.api :as d]
            [kanopi.test-util :as test-util]
            [kanopi.web.auth :refer :all]))

(deftest register
  (let [sys (-> (test-util/system-excl-web)
                (component/start))
        username "brian"
        password "rubinton"
        res @(register! (:authenticator sys) username password)
        creds (credentials (:authenticator sys) username)]
    (is (not-empty (:tx-data res)))
    (is (= username (:username creds)))
    (is (d/entity (d/db (get-in sys [:datomic-peer :connection]))
                  (:ent-id creds)))
    (is (creds/bcrypt-verify password (:password creds)))

    (component/stop sys)))

(deftest change-password
  (testing ""
    (let []
      )))

;; TODO: test registering existing username
