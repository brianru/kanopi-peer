(ns kanopi.web.app-test
  (:require [clojure.test :refer :all]
            [kanopi.test-util :as test-util]
            [com.stuartsierra.component :as component]
            [liberator.dev]
            [datomic.api :as d]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as base64]
            [clj-time.core :as time]
            [kanopi.web.app :as web-app]
            [kanopi.web.auth :as auth]))

(deftest authentication
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey" :password "mouse"}
        test-ent-ids
        (d/q '[:find [?eid ...] :in $ :where [?eid :thunk/label _]]
             (d/db (get-in system [:datomic-peer :connection])))
        ]

    (testing "unauthorized-access"
      (let [req  (mock/request :get "/")
            resp (handler req)]
        (is (= 302 (:status resp)))))

    (testing "unauthorized-login"
      (let [req   (-> (mock/request :post "/login" creds)) 
            resp  (handler req)]
        (is (= 302 (:status resp)))
        (is (re-find #"login_failed" (get-in resp [:headers "Location"])))))

    (testing "register"
      (let [req   (mock/request :post "/register" creds)
            resp  (handler req)]
        (is (= 303 (:status resp)))
        (is (re-find #"welcome=true" (get-in resp [:headers "Location"])))
        (is (auth/verify-creds (:authenticator system) creds))))

    (testing "login-redirect"
      (let [req (mock/request :post "/login" creds)
            resp (handler req)]
        (is (= 303 (:status resp)))
        (is (= "http://localhost/" (get-in resp [:headers "Location"])))))

    (testing "access-spa-creds"
      (let [req  (-> (mock/request :get "/")
                     (test-util/assoc-basic-auth creds)) 
            resp (handler req)]
        (is (= 200 (:status resp)))
        (is (re-find #"<title>kanopi</title>" (:body resp)))))

    (testing "access-api"
      (let [req (-> (mock/request :get "/api/"
                                  {:ent-id (first test-ent-ids)
                                   :verb :get
                                   :place :unit-test
                                   :time (time/now)
                                   })
                    (test-util/assoc-basic-auth creds))
            resp (handler req)
            resp-body (read-string (:body resp))]
        (is (not-empty (get resp-body :focus-entity)))))

    (component/stop system)))
