(ns kanopi.client.session-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            
            [schema.core :as s]

            [kanopi.system.server :as server]
            [kanopi.system.client :as client]
            
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as message]
            [kanopi.model.session :as session]

            [kanopi.controller.handlers.request :as request]
            [kanopi.controller.authenticator :as authenticator]

            [kanopi.test-macros :refer :all]
            [kanopi.test-util :as test-util]))

(deftest well-formed-anonymous-session
  (let [server-system     (component/start (test-util/system-excl-web))
        anonymous-session (session/init-anonymous-session (get server-system :session-service))

        client-system (-> (client/new-system {:app-state
                                              {:initial-value anonymous-session}})
                          (component/start)) 
        client-app-state (-> client-system :app-state :app-state (deref))
        ]

    (is (not (s/check schema/ClientSession anonymous-session))) 
    (is (not (s/check schema/ClientSession client-app-state)))

    (component/stop server-system)
    (component/stop client-system)
    ))

(deftest well-formed-authenticated-session
  (let [server-system (component/start (test-util/system-excl-web))
        creds (let [auth-svc (:authenticator server-system)]
                  (authenticator/register! auth-svc "brian" "rubinton")
                  (authenticator/credentials auth-svc "brian"))
        user-session (session/init-session (get server-system :session-service) creds)

        client-system (-> (client/new-system {:app-state {:initial-value user-session}})
                          (component/start)) 
        client-app-state (-> client-system :app-state :app-state (deref))
        ]

    (is (not (s/check schema/ClientSession user-session)))
    (is (not (s/check schema/ClientSession client-app-state)))

    (component/stop server-system)
    (component/stop client-system)))
