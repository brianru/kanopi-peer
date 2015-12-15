(ns kanopi.model.session-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            
            [com.stuartsierra.component :as component]
            [schema.core :as s]

            [datomic.api :as d]

            [kanopi.model.schema :as schema]
            [kanopi.model.session :as session]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.model.data.impl :as data-impl]

            [kanopi.controller.authenticator :as authenticator]
            
            [kanopi.util.core :as util]
            [kanopi.test-util :as test-util]
            ))

(deftest init-anonymous-session
  (let [{:keys [session-service authenticator] :as system}
        (component/start (test-util/system-excl-web-server))
        ]
    (testing "anonymous user"
      (let [ses (session/init-anonymous-session session-service)]
        (is (not-empty (get ses :user)))
        ; not working for some reason
        ; (is (s/check schema/Credentials (get ses :user)))
        (is (s/validate schema/Credentials (get ses :user)))
        (is (not-empty (get ses :cache)))
        (is (not-empty (get ses :page)))
        (is (not-empty (get ses :datum)))
        ; (is (->> ses
        ;          ((juxt :most-edited-datums :recent-datums))
        ;          (every? empty?)))
        ))

    (testing "registered user"
      (let [creds (do (authenticator/register! authenticator "brian" "rubinton")
                      (authenticator/credentials authenticator "brian"))
            ses   (session/init-session session-service creds)]
        (is (not-empty (get ses :user)))
        (is (= (dissoc creds :password) (get ses :user)))
        ; (is (->> ses
        ;          ((juxt :most-edited-datums :recent-datums))
        ;          (every? not-empty)))
        ))
    (component/stop system)))

(deftest session-cache
  (let [{:keys [session-service authenticator] :as system}
        (component/start (test-util/system-excl-web-server))
        ]
    (testing "anon session"
      (let [ses (session/init-anonymous-session session-service)
            db  (test-util/get-db system)
            dtm-id (get-in ses [:datum :datum :db/id])
            related-ents (data-impl/get-related-entity-ids db dtm-id)]
        (is (nil? (s/check schema/DatomicId dtm-id)))
        (is (not-empty related-ents))
        ))

    (component/stop system)))
