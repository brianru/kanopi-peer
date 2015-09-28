(ns kanopi.web.app-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]

            [com.stuartsierra.component :as component]
            [datomic.api :as d]

            [liberator.dev]
            [ring.mock.request :as mock]
            [org.httpkit.client :as http]

            [cheshire.core :as json]
            [clojure.data.codec.base64 :as base64]
            [clj-time.core :as time]

            [kanopi.test-util :as test-util]
            [kanopi.util.core :as util]
            [kanopi.system :as sys]
            [kanopi.main :as main]
            [kanopi.web.app :as web-app]
            [kanopi.web.auth :as auth]
            ))

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
      (let [req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "application/transit+json")) 
            resp  (handler req)]
        (is (= 401 (:status resp)))
        #_(is (re-find #"login_failed" (get-in resp [:headers "Location"] "")))))

    (testing "register"
      (let [req   (-> (mock/request :post "/register" creds)
                      (mock/header :accept "application/transit+json"))
            resp  (handler req)
            body  (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (not-empty body))
        #_(is (re-find #"welcome=true" (get-in resp [:headers "Location"] "")))
        (is (auth/verify-creds (:authenticator system) creds))))

    (testing "login-success"
      (let [req (-> (mock/request :post "/login" creds)
                    (mock/header :accept "application/transit+json"))
            resp (handler req)]
        (is (= 200 (:status resp)))
        (is (not-empty (util/transit-read (:body resp))))))

    (testing "login-redirect"
      (let [req (-> (mock/request :post "/login" creds)
                    (mock/header :accept "text/html"))
            resp (handler req)]
        (is (= 303 (:status resp)))
        (is (= "http://localhost/?welcome=true" (get-in resp [:headers "Location"] "")))))

    (testing "access-spa-creds"
      (let [req  (-> (mock/request :get "/")
                     (test-util/assoc-basic-auth creds)) 
            resp (handler req)]
        (is (= 200 (:status resp)))
        (is (re-find #"<title>kanopi</title>" (:body resp)))))

    (component/stop system)))

(deftest session-management
  (let [system (component/start (test-util/system-excl-web-server)
                                ;(sys/new-system main/default-config)
                                )
        handler (get-in system [:web-app :app-handler])
        ]
    (testing "register: transit+json response"
      (let [creds  {:username "minney" :password "mouse"}
            req (-> (mock/request :post "/register" creds)
                    (mock/header :accept "application/transit+json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (util/transit-read body)
            ]
        (is (= 200 status))
        (is (= (:username creds) (:username body')))))

    (testing "register: http response"
      (let [
            creds  {:username "homer" :password "simpson"}
            req (-> (mock/request :post "/register" creds)
                    (mock/header :accept "text/html"))
            {:keys [status headers body] :as resp} (handler req)
            ]
        (is (= 303 status))
        (is (re-find #"welcome=true" (get-in headers ["Location"] "")))))

    (testing "login: transit+json"
      (let [creds {:username "minney" :password "mouse"}
            req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "application/transit+json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (util/transit-read body)
            cookie (-> (get headers "Set-Cookie") (first)) 
            ]
        (is (= 200 status))
        (is (:username creds) (:username body))
        (is (not-empty cookie))
        (let [msg {:foo :bar}
              req (-> (mock/request :post "/api" msg)
                      (mock/header :accept "application/transit+json")
                      (mock/header :cookie cookie))
              {:keys [status headers body]} (handler req)
              body' (util/transit-read body)]
          (is (= 200 status))
          (is (= msg body')))))

    (testing "login: text/html"
      (let [creds {:username "homer" :password "simpson"}
            req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "text/html"))
            {:keys [status headers body] :as resp} (handler req)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (= 303 status))
        (is (re-find #"welcome=true" (get-in headers ["Location"] "")))
        (is (not-empty cookie))
        (let [msg {:foo :bar}
              req (-> (mock/request :post "/api" msg)
                      (mock/header :accept "application/transit+json")
                      (mock/header :cookie cookie)
                      )
              {:keys [status headers body]} (handler req)
              body' (util/transit-read body)]
          (is (= 200 status))
          (is (= msg body')))))

    (component/stop system)))

(deftest message-passing-api
  (let [system  (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey", :password "mouse"}
        _       (-> (mock/request :post "/register" creds)
                    (mock/header :accept "application/transit+json")
                    (handler))
        thunk-ent-ids
        (d/q '[:find [?eid ...] :in $ :where [?eid :thunk/label _]]
             (d/db (get-in system [:datomic-peer :connection])))]

    (testing "get-thunk-failure"
      (let [message {:noun -1000
                     :verb :get-thunk
                     :context {}}
            req (-> (mock/request :post "/api" message)
                    (test-util/assoc-basic-auth creds))
            resp (handler req)
            body (util/transit-read (:body resp))
            ]
        (is (= 200 (:status resp)))
        (is (= :get-thunk-failure (get body :verb)))
        (is (nil? (get-in body [:noun :thunk])))
        ))

    (testing "get-thunk-success"
      (let [test-ent-id (first thunk-ent-ids)
            message {:noun test-ent-id
                     :verb :get-thunk
                     :context {}}
            req (-> (mock/request :post "/api" message)
                    (test-util/assoc-basic-auth creds))
            resp (handler req)
            body (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (= :get-thunk-success (get body :verb)))
        (is (= test-ent-id (-> body :noun :thunk :db/id first)))
        ;; NOTE: not testing similar-thunks and context-thunks 
        ;; contents here because their existence depends on the
        ;; particular test-ent-id
        ))

    (testing "update-thunk-label"
      (let []
        ))

    (testing "update-fact"
      (let []
        ))

    (component/stop system)))




