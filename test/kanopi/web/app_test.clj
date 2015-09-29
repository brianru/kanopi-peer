(ns kanopi.web.app-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]

            [com.stuartsierra.component :as component]
            [datomic.api :as d]

            [liberator.dev]
            [ring.mock.request :as mock]
            [org.httpkit.client :as http]

            [cheshire.core :as json]
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
        ))
    (testing "unauthorized-login html"
      (let [req (-> (mock/request :post "/login" creds)
                    (mock/header :accept "text/html"))
            resp (handler req)
            ]
        (is (= 401 (:status resp)))
        #_(is (re-find #"login_failed" (get-in resp [:headers "Location"] "")))))

    (testing "register"
      (let [req   (-> (mock/request :post "/register" creds)
                      (mock/header :accept "application/transit+json"))
            resp  (handler req)
            body  (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (= (:username creds) (:username body)))
        #_(is (re-find #"welcome=true" (get-in resp [:headers "Location"] "")))
        (is (auth/verify-creds (:authenticator system) creds))))

    (testing "login-success"
      (let [req (-> (mock/request :post "/login" creds)
                    (mock/header :accept "application/transit+json"))
            resp (handler req)
            body' (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (= (:username creds) (:username body')))
        ))

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
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler]) ]

    (testing "new session each time"
      (let [register (fn [username password]
                       (-> (mock/request :post "/register"
                                         {:username username, :password password})
                           (mock/header :accept "text/html")))
            responses (map (comp handler register) (range 400 410) (range 300 310))
            response-cookies
            (->> responses
                 (mapcat (comp
                          #(map (comp rest (partial re-find #"([^=]+)=(.+);Path=")) %)
                          #(-> % (get-in [:headers "Set-Cookie"]))))
                 (doall))
            cookie-names (->> response-cookies (map first) (set))
            cookie-values (->> response-cookies (map last) (set))]
        (is (= cookie-names #{"kanopi-session"}))
        (is (= 10 (count cookie-values)))))

    (testing "register: transit+json response"
      (let [creds  {:username "minney" :password "mouse"}
            req (-> (mock/request :post "/register" creds)
                    (mock/header :accept "application/transit+json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (util/transit-read body)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (= 200 status))
        (is (= (:username creds) (:username body')))
        (is (not-empty cookie))
        (let [msg {:noun :fizzbuzz
                   :verb :fobar
                   :context {}}
              req (-> (mock/request :post "/api" msg)
                      (mock/header :accept "application/transit+json")
                      (mock/header :cookie cookie))
              {:keys [status headers body]} (handler req)
              body' (util/transit-read body)
              ]
          (is (= 200 status))
          (is (= (select-keys msg [:noun :verb]) (select-keys body' [:noun :verb])))
          )))

    (testing "register: http response"
      (let [cookie-zero (-> (mock/request :get "/register")
                            (handler)
                            (get-in [:headers "Set-Cookie"]))
            creds  {:username "homer" :password "simpson"}
            req (-> (mock/request :post "/register" creds)
                    (mock/header :accept "text/html"))
            {:keys [status headers body] :as resp} (handler req)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (re-find #"kanopi-init" (first cookie-zero)))
        (is (= 200 status))
        #_(is (= 303 status))
        #_(is (re-find #"welcome=true" (get-in headers ["Location"] "")))
        (is (not-empty cookie))
        (let [msg {:noun :fizzbuzz
                   :verb :foobar}
              req (-> (mock/request :post "/api" msg)
                      (mock/header :accept "application/transit+json")
                      (mock/header :cookie cookie))
              {:keys [status headers body]} (handler req)
              body' (util/transit-read body)]
          (is (= 200 status))
          (is (= (select-keys msg [:noun :verb])
                 (select-keys body' [:noun :verb]))))))

    (testing "login: transit+json and post a message to api"
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
        (let [msg {:noun :foobar
                   :verb :testing123}
              req (-> (mock/request :post "/api" msg)
                      (mock/header :accept "application/transit+json")
                      (mock/header :cookie cookie))
              {:keys [status headers body]} (handler req)
              body' (util/transit-read body)]
          (is (= 200 status))
          (is (= (select-keys msg [:noun :verb])
                 (select-keys body' [:noun :verb]))))))

    (testing "login: text/html and post a message to api"
      (let [creds {:username "homer" :password "simpson"}
            req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "text/html"))
            {:keys [status headers body] :as resp} (handler req)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (= 303 status))
        (is (re-find #"welcome=true" (get-in headers ["Location"] "")))
        (is (not-empty cookie))
        (let [msg {:noun :foobar
                   :verb :testingplay}
              req (-> (mock/request :post "/api" msg)
                      (mock/header :accept "application/transit+json")
                      (mock/header :cookie cookie))
              {:keys [status headers body]} (handler req)
              body' (util/transit-read body)]
          (is (= 200 status))
          (is (= (select-keys msg [:noun :verb])
                 (select-keys body' [:noun :verb])
                 )))))

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
        (is (= test-ent-id (-> body :noun :thunk :db/id)))
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

