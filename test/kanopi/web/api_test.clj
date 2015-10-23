(ns kanopi.web.api-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]

            [com.stuartsierra.component :as component]
            [datomic.api :as d]

            [ring.mock.request :as mock]

            [kanopi.main :as main]
            [kanopi.system :as sys]
            [kanopi.data :as data]

            [kanopi.web.app :as web-app]

            [kanopi.util.core :as util]
            [kanopi.test-util :as test-util]
            ))

(deftest get-datum
  (let [system   (component/start (test-util/system-excl-web-server))
        creds    {:username "mickey", :password "mouse132"}
        resp     (test-util/mock-register system creds)
        test-datum-ent-id (d/q '[:find ?eid .
                                 :in $
                                 :where [?eid :datum/label _]]
                               (test-util/get-db system))
        ]

    (testing "get-datum-failure"
      (let [message {:noun -1000
                     :verb :get-datum
                     :context {}}
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" message :creds creds)
            ]
        (is (= 200 (:status resp)))
        (is (= :get-datum-failure (get body :verb)))
        (is (nil? (get-in body [:noun :datum])))
        ))

    (testing "get-datum-success"
      (let [message {:noun test-datum-ent-id
                     :verb :get-datum
                     :context {}}
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" message :creds creds)
            ]
        (is (= 200 (:status resp)))
        (is (= :get-datum-success (get body :verb)))
        (is (= test-datum-ent-id (-> body :noun :datum :db/id)))
        ))
    ))

(deftest update-datum
  (let [system   (component/start (test-util/system-excl-web-server))
        data-svc (get system :data-service)
        handler  (get-in system [:web-app :app-handler])
        creds    {:username "mickey", :password "mouse132"}
        resp     (test-util/mock-register system creds)
        test-ent-id (d/q '[:find ?eid .
                           :in $
                           :where [?eid :datum/label _]]
                         (test-util/get-db system))
        ]

    (testing "update-datum-label"
      (let [lbl' "duuuude"
            message {:noun {:existing-entity test-ent-id
                            :new-label lbl'}
                     :verb :update-datum-label
                     :context {}}
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" (util/transit-write message)
                                     :content-type "application/transit+json"
                                     :creds creds)
            ]
        (is (= 200 (:status resp)))
        (is (= :update-datum-label-success (get body :verb)))
        (is (= lbl' (-> body :noun :datum/label)))
        ))

    ))


(deftest app-and-update-facts
  (let [system   (component/start (test-util/system-excl-web-server))
        data-svc (get system :data-service)
        handler  (get-in system [:web-app :app-handler])
        creds    {:username "mickey", :password "mouse132"}
        resp     (test-util/mock-register system creds)
        test-ent-id (d/q '[:find ?eid .
                           :in $
                           :where [?eid :datum/label _]]
                         (test-util/get-db system))
        ]

    (testing "add then update fact"
      (let [test-ent (data/get-datum data-svc creds test-ent-id)
            fact' ["age" 42]
            message {:noun {:datum-id test-ent-id
                            :fact {:fact/attribute {:literal/text "age"}
                                   :fact/value {:literal/integer 42}}}
                     :verb :update-fact
                     :context {}}
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" (util/transit-write message)
                                     :creds creds
                                     :content-type "application/transit+json")
            test-ent' (data/get-datum data-svc creds test-ent-id)
            old-facts (-> test-ent :datum/fact set)

            [new-fact & _ :as new-facts]
            (->> (clojure.set/difference (-> test-ent' :datum/fact set) old-facts)
                 (into (list))) 
            ]
        (is (= 200 (:status resp)))
        (is (= :update-fact-success (get body :verb)))
        (is (not-empty new-facts))
        (is (= 1 (count new-facts)))
        (is (= ["age" 42] (util/fact-entity->tuple new-fact)))

        (let [message {:noun {:datum-id test-ent-id
                              :fact (assoc-in new-fact [:fact/value :literal/integer] 17)}
                       :verb :update-fact
                       :context {}}
              {:keys [body] :as resp}
              (test-util/mock-request! system :post "/api" (util/transit-write message)
                                       :creds creds
                                       :content-type "application/transit+json")
              test-ent'' (data/get-datum data-svc creds test-ent-id)
              updated-facts (clojure.set/difference
                             (-> test-ent'' :datum/fact set)
                             (-> test-ent'  :datum/fact set)) ]
          (is (= 200 (:status resp)))
          (is (= :update-fact-success (get body :verb)))
          (is (not-empty updated-facts))
          (is (= 1 (count updated-facts)))
          (is (= ["age" 17] (util/fact-entity->tuple (first updated-facts))))
          )
        ))

    ))

(deftest initialize-client-state
  (let [system   (component/start (test-util/system-excl-web-server))
        data-svc (get system :data-service)
        handler  (get-in system [:web-app :app-handler])
        creds    {:username "mickey", :password "mouse132"}
        resp     (test-util/mock-register system creds)]
    (testing "success"
      (let []
        ))
    ))
