(ns kanopi.web.app-test
  (:require [clojure.test :refer :all]
            [kanopi.test-util :as test-util]
            [kanopi.util.core :as util]
            [com.stuartsierra.component :as component]
            [liberator.dev]
            [datomic.api :as d]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as base64]
            [clj-time.core :as time]
            [kanopi.web.app :as web-app]
            [kanopi.web.auth :as auth]))

;; Test JSON/EDN/Transit auth API
(deftest authentication
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey" :password "mouse"}
        test-ent-ids
        (d/q '[:find [?eid ...] :in $ :where [?eid :thunk/label _]]
             (d/db (get-in system [:datomic-peer :connection])))
        ]

    (testing "access-spa-anonymously"
      (let [req  (mock/request :get "/")
            resp (handler req)]
        (is (= 200 (:status resp)))))

    (testing "unauthorized-login"
      (let [req   (-> (mock/request :post "/login" creds)) 
            resp  (handler req)]
        (is (= 401 (:status resp)))
        #_(is (re-find #"login_failed" (get-in resp [:headers "Location"])))))

    (testing "register"
      (let [req   (mock/request :post "/register" creds)
            resp  (handler req)
            body  (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (not-empty body))
        (println "resp body!" body)
        #_(is (re-find #"welcome=true" (get-in resp [:headers "Location"])))
        (is (auth/verify-creds (:authenticator system) creds))))

    (testing "login-success"
      (let [req (mock/request :post "/login" creds)
            resp (handler req)]
        (is (= 200 (:status resp)))
        (is (not-empty (util/transit-read (:body resp))))))

    #_(testing "login-redirect"
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

    (component/stop system)))

#_(deftest message-passing-api
  (let [system  (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey", :password "mouse"}]

    (testing "get-thunk"
      (let [req (mock/request :post "/api" {:noun -1000
                                            :verb :get-thunk
                                            :context {}})
            resp (handler req)
            body (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (not-empty body))
        ))

    (testing "update-thunk-label"
      (let []
        ))

    (testing "update-fact"
      (let []
        ))

    (component/stop system)))




