(ns kanopi.web.app-test
  (:require [clojure.test :refer :all]
            [kanopi.test-util :as test-util]
            [com.stuartsierra.component :as component]
            [liberator.dev]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as base64]
            [kanopi.web.app :as web-app]
            [kanopi.web.auth :as auth]))

(deftest authentication
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey" :password "mouse"}]

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
      (let [req  (-> (mock/request :get "/"; {:basic-auth ["mickey" "mouse"]}
                                   )
                     (assoc-in [:headers "authorization"]
                               (test-util/mk-basic-auth-header creds))) 
            resp (handler req)
            ]
        (is (= 200 (:status resp)))
        (is (re-find #"<title>kanopi</title>" (:body resp)))
        
        ))

    (component/stop system)))
