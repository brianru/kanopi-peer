(ns kanopi.web.app-test
  (:require [clojure.test :refer :all]
            [kanopi.test-util :as test-util]
            [com.stuartsierra.component :as component]
            [liberator.dev]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [kanopi.web.app :as web-app]
            [kanopi.web.auth :as auth]))

(deftest authentication
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])]
    (is handler)

    (testing "unauthorized-access"
      (let [req (mock/request :get "/")
            resp (handler req)]
        (is (= 302 (:status resp)))))

    (testing "unauthorized-login"
      (let [creds {"username" "mickey" "password" "mouse"}
            req (-> (mock/request :post "/login" creds)) 
            resp (handler req)]
        (is (= 302 (:status resp)))
        (is (re-find #"login_failed" (get-in resp [:headers "Location"])))))

    (testing "register"
      (let [creds (json/generate-string {"username" "mickey" "password" "mouse"})
            req (-> (mock/request :post "/register" {"username" "mickey" "password" "mouse"}))
            resp (handler req)]
        (is (= 303 (:status resp)))
        (is (re-find #"welcome=true" (get-in resp [:headers "Location"])))
        (is (auth/verify-creds (:authenticator system)
                               {:username "mickey" :password "mouse"}))))

    (component/stop system)))
