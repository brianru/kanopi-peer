(ns kanopi.model.session-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            
            [com.stuartsierra.component :as component]
            [schema.core :as s]

            [datomic.api :as d]

            [kanopi.model.schema :as schema]
            [kanopi.model.session :as session]

            [kanopi.controller.authenticator :as authenticator]
            
            [kanopi.util.core :as util]
            [kanopi.test-util :as test-util]
            ))

(deftest hypothetical-db
  (let [{:keys [session-service authenticator] :as system}
        (component/start (test-util/system-excl-web-server))

        creds (authenticator/temp-user)
        txdata (authenticator/-init-team-data
                authenticator (get-in creds [:current-team :db/id]))
        ]
    (testing "can I access the user's stuff?"
      (let [db (session/-hypothetical-db session-service creds txdata)
            ]
        (is (d/q '[:find ?e .
                   :in $
                   :where [?e :datum/label _]]
                 db))))))

(deftest init-session
  (let [{:keys [session-service authenticator] :as system}
        (component/start (test-util/system-excl-web-server))
        ]
    (testing "anonymous user"
      (let [ses (session/init-session session-service)]
        (pprint ses)
        (is (not-empty (get ses :user)))
        (is (s/check schema/Credentials (get ses :user)))
        (is (not-empty (get ses :cache)))
        (is (not-empty (get ses :page)))
        (is (not-empty (get ses :datum)))
        (is (->> ses
                 ((juxt :most-viewed-datums :most-edited-datums :recent-datums))
                 (every? empty?)))))

    (testing "registered user"
      (let [creds (authenticator/register! authenticator "brian" "rubinton")
            ses   (session/init-session session-service creds)]
        (is (not-empty (get ses :user)))
        (is (= creds (get ses :user)))
        (is (->> ses
                 ((juxt :most-viewed-datums :most-edited-datums :recent-datums))
                 (every? not-empty)))
        ))
    (component/stop system)))
