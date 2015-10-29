(ns kanopi.view.api-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]

            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [schema.core :as s]

            [ring.mock.request :as mock]

            [kanopi.main :as main]
            [kanopi.model.data :as data]

            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]
            [kanopi.view.web-app :as web-app]

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
      (let [message (msg/get-datum -1000)
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" message :creds creds)
            ]
        (is (= 200 (:status resp)))
        (is (= :get-datum-failure (get body :verb)))
        (is (nil? (get-in body [:noun :datum])))
        ))

    (testing "get-datum-success"
      (let [message (msg/get-datum test-datum-ent-id)
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" message :creds creds)
            ]
        (is (= 200 (:status resp)))
        (is (= :get-datum-success (get body :verb)))
        (is (= test-datum-ent-id (-> body :noun :datum :db/id)))
        ))

    (component/stop system)))

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
            message (msg/update-datum-label test-ent-id lbl')
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" (util/transit-write message)
                                     :content-type "application/transit+json"
                                     :creds creds)
            ]
        (is (= 200 (:status resp)))
        (is (= :update-datum-label-success (get body :verb)))
        (is (= lbl' (-> body :noun :datum/label)))
        ))

    (component/stop system)))


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
            fact' {:fact/attribute {:literal/text "age"}
                   :fact/value     {:literal/integer 42}}
            message (msg/update-fact test-ent-id fact')
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

        (let [fact'' (assoc-in new-fact [:fact/value :literal/integer] 17)
              message (msg/update-fact test-ent-id fact'')
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
   (component/stop system)))

(deftest initialize-client-state
  (let [system   (component/start (test-util/system-excl-web-server))
        data-svc (get system :data-service)
        handler  (get-in system [:web-app :app-handler])
        creds    {:username "mickey", :password "mouse132"}
        resp     (test-util/mock-register system creds)]
    (testing "success"
      (let [message (msg/initialize-client-state creds)
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" (util/transit-write message)
                                     :creds creds
                                     :content-type "application/transit+json")
            ]
        (is (= 200 (:status resp)))
        (is (= :initialize-client-state-success (:verb body)))
        (is (every? (partial s/validate schema/Datum)
                    (vals (get-in body [:noun :cache]))))
        ))
    (component/stop system)))

;; FIXME: search fails here but succeeds in data-test
(deftest search
  (let [system (component/start (test-util/system-excl-web-server))
        data-svc (get system :data-service)
        handler (get-in system [:web-app :app-handler])
        creds {:username "brian" :password "rubinton"}
        resp  (test-util/mock-register system creds)]
    (testing "success"
      (let [message (msg/search creds "lobster")
            {:keys [body] :as resp}
            (test-util/mock-request! system :post "/api" (util/transit-write message)
                                     :creds creds
                                     :content-type "application/transit+json")
            ]
        (is (= 200 (:status resp)))
        (is (= :search-success (:verb body)))
        (is (not-empty (:noun body)))))))

