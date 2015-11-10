(ns kanopi.model.session-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            
            [com.stuartsierra.component :as component]
            [schema.core :as s]

            [kanopi.model.schema :as schema]
            [kanopi.model.session :as session]
            
            [kanopi.util.core :as util]
            [kanopi.test-util :as test-util]
            ))

(deftest init-session
  (let [system (component/start (test-util/system-excl-web-server))
        session-service (get-in system [:session-service])]
    (testing "anonymous user"
      (let [ses (session/init-session session-service)]
        (is (not-empty (get ses :user)))
        (is (s/check schema/Credentials (get ses :user)))
        (is (not-empty (get ses :cache)))))

    (testing "registered user"
      (let []
        ))
    (component/stop system)))
