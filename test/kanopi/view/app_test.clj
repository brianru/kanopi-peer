(ns kanopi.view.app-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]

            [com.stuartsierra.component :as component]
            [datomic.api :as d]

            [liberator.dev]
            [ring.mock.request :as mock]

            [cheshire.core :as json]
            [clj-time.core :as time]

            [kanopi.test-util :as test-util]
            [kanopi.util.core :as util]
            [kanopi.main :as main]
            [kanopi.model.data :as data]
            [kanopi.controller.authenticator :as auth]
            [kanopi.view.web-app :as web-app]

            [schema.experimental.generators :as generators]
            [kanopi.model.schema :as schema]))


;; Test JSON auth API
;; FIXME does not work because login relies on friend, which relies on the web
;; server
(deftest authentication
  (let [system  (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])
        creds   {:username "mickey" :password "mouse123"}
        creds1  {:username "mickeymouse" :password "minneymouse"}
        test-ent-ids
        (d/q '[:find [?eid ...] :in $ :where [?eid :datum/label _]]
             (d/db (get-in system [:datomic-peer :connection])))]

    (testing "unauthorized-login"
      (let [req  (-> (mock/request :post "/login" creds)
                     (mock/header :accept "application/json"))
            resp (handler req)]
        (is (= 401 (:status resp)))))

    (testing "register"
      (let [req  (-> (mock/request :post "/register" creds)
                     (mock/header :accept "application/json"))
            resp (handler req)
            body (json/parse-string (:body resp) keyword)]
        (is (= 200 (:status resp)))
        (is (= (:username creds) (:username body)))
        (is (auth/verify-creds (:authenticator system) creds))))

    (testing "login"
      (let [req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "application/json"))
            resp  (handler req)
            body' (json/parse-string (:body resp) keyword)]
        (is (= 200 (:status resp)))
        (is (= (:username creds) (:username body')))))

    (component/stop system)))

(deftest session-management
  (let [system (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])]

    (testing "new session each time"
      (let [register (fn [[username password]]
                       (-> (mock/request :post "/register"
                                         {:username username, :password password})
                           (mock/header :accept "application/json")))
            sample-creds (map vector
                              (map str (range 10000000 10000010))
                              (map str (range 20000000 20000010)))
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

    (testing "register: json response"
      (let [creds  {:username "minney" :password "mouse123"}
            req (-> (mock/request :post "/register" creds)
                    (mock/header :accept "application/json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (json/parse-string body keyword)
            cookie (-> (get headers "Set-Cookie") (first))
            ]
        (is (= 200 status))
        (is (= (:username creds) (:username body')))
        (is (not-empty cookie))
        #_(let [msg {:noun 42
                     :verb :fobar
                     :context {}}
                req (-> (mock/request :post "/api" msg)
                        (mock/header :accept "application/json")
                        (mock/header :cookie cookie))
                {:keys [status headers body]} (handler req)
                body' (json/parse-string body keyword)]
            (is (= 200 status))
            (is (= (select-keys msg [:noun :verb]) (select-keys body' [:noun :verb]))))))

    (testing "login: json"
      (let [creds {:username "minney" :password "mouse123"}
            req   (-> (mock/request :post "/login" creds)
                      (mock/header :accept "application/json"))
            {:keys [status headers body] :as resp} (handler req)
            body' (json/parse-string body keyword)
            cookie (-> (get headers "Set-Cookie") (first))]
        (is (= 200 status))
        (is (:username creds) (:username body))
        (is (not-empty cookie))
        #_(let [msg {:noun 42
                     :verb :testing123}
                req (-> (mock/request :post "/api" msg)
                        (mock/header :accept "application/transit+json")
                        (mock/header :cookie cookie))
                {:keys [status headers body]} (handler req)
                body' (util/transit-read body)]
            (is (= 200 status))
            (is (= (select-keys msg [:noun :verb])
                   (select-keys body' [:noun :verb]))))))

    (component/stop system)))
