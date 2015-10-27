(ns kanopi.view.app-test
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
            [kanopi.main :as main]
            [kanopi.model.data :as data]
            [kanopi.controller.auth :as auth]
            [kanopi.view.web-app :as web-app]

            [schema.experimental.generators :as generators]
            [kanopi.model.schema :as schema]
            ))

;; Test JSON/EDN/Transit auth API
(deftest authentication
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey" :password "mouse123"}
        creds1  {:username "mickeymouse" :password "minneymouse"}
        test-ent-ids
        (d/q '[:find [?eid ...] :in $ :where [?eid :datum/label _]]
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
        #_(is (re-find #"login?fail=true" (get-in resp [:headers "Location"] "")))))

    (testing "register-transit"
      (let [req   (-> (mock/request :post "/register" creds)
                      (mock/header :accept "application/transit+json"))
            resp  (handler req)
            body  (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (= (:username creds) (:username body)))
        (is (auth/verify-creds (:authenticator system) creds))
        ))

    (testing "register-html"
      (let [req  (-> (mock/request :post "/register" creds1)
                     (mock/header :accept "text/html"))
            resp (handler req)
            ]
        (is (= 301 (:status resp)))
        (is (re-find #"welcome=true" (get-in resp [:headers "Location"] "")))
        (is (auth/verify-creds (:authenticator system) creds1))
        ))

    (testing "login-transit"
      (let [req (-> (mock/request :post "/login" creds)
                    (mock/header :accept "application/transit+json"))
            resp  (handler req)
            body' (util/transit-read (:body resp))]
        (is (= 200 (:status resp)))
        (is (= (:username creds) (:username body')))
        ))

    (testing "login-html"
      (let [req (-> (mock/request :post "/login" creds)
                    (mock/header :accept "text/html"))
            resp (handler req)]
        (is (= 303 (:status resp)))
        (is (= "http://localhost/?welcome=true" (get-in resp [:headers "Location"] "")))
        ))

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
      (let [register (fn [[username password]]
                       (-> (mock/request :post "/register"
                                         {:username username, :password password})
                           (mock/header :accept "text/html")))
            sample-creds (map vector
                              (map str (range 10000000 10000010))
                              (map str (range 20000000 20000010)))
            _ (println "HERe" sample-creds)
            responses (map (comp handler register) sample-creds)
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
      (let [creds  {:username "minney" :password "mouse123"}
            req (-> (mock/request :post "/register" creds)
                    (mock/header :accept "application/transit+json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (util/transit-read body)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (= 200 status))
        (is (= (:username creds) (:username body')))
        (is (not-empty cookie))
        (let [msg {:noun 42
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
            creds  {:username "homer" :password "simpsondude"}
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
        (let [msg {:noun 42
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
      (let [creds {:username "minney" :password "mouse123"}
            req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "application/transit+json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (util/transit-read body)
            cookie (-> (get headers "Set-Cookie") (first)) 
            ]
        (is (= 200 status))
        (is (:username creds) (:username body))
        (is (not-empty cookie))
        (let [msg {:noun 42
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
      (let [creds {:username "homer" :password "simpsondude"}
            req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "text/html"))
            {:keys [status headers body] :as resp} (handler req)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (= 303 status))
        (is (re-find #"welcome=true" (get-in headers ["Location"] "")))
        (is (not-empty cookie))
        (let [msg {:noun 42
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
