(ns kanopi.controller.handlers.request-test
  "Test methods for kanopi.controller.handlers.request/local-request-handler"
  (:require [clojure.test :refer :all]
            [kanopi.controller.handlers.request :refer :all]
            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]))

(deftest spa-navigate
  (let [app-state {}]
    (testing "to datum"
      (let [msg (msg/navigate {:handler :datum
                               :route-params {:id -42}})]
        (is (= [(msg/navigate-success (:noun msg)) (msg/get-datum -42)]
               (:messages (local-request-handler app-state msg))))))
    (testing "to literal"
      (let [msg (msg/navigate {:handler :literal
                               :route-params {:id "apple"}})]
        (is (= [(msg/navigate-success (:noun msg)) (msg/get-literal "apple")]
               (:messages (local-request-handler app-state msg))))))))


(deftest spa-switch-team
  (let [initial-app-state {:user {:teams [{:team/id "abc"}
                                          {:team/id "456"}
                                          {:team/id "team1"}]
                                  :current-team {:team/id "team1"}}}
        app-state (atom initial-app-state)]
    (testing "switching to a non-existent team"
      (let [msg (msg/switch-team "not a team")]
        (is (= [(msg/switch-team-success (-> app-state deref :user))]
               (:messages (local-request-handler app-state msg))))
        (is (= "team1" (-> app-state deref :user :current-team :team/id)))))
    (testing "switch to an available team"
      (let [msg (msg/switch-team "abc")
            new-state (swap! app-state update :user
                             #(assoc % :current-team {:team/id "abc"}))]
        (is (= [(msg/switch-team-success (-> app-state deref :user))]
               (:messages (local-request-handler app-state msg))))
        (is (= "abc" (-> app-state deref :user :current-team :team/id)))))
    (testing "switch back"
      (let [msg (msg/switch-team (get-in initial-app-state [:user :current-team :team/id]))
            new-state (swap! app-state update :user
                             #(assoc % :current-team {:team/id (:noun msg)}))]
        (is (= [(msg/switch-team-success (-> app-state deref :user))]
               (:messages (local-request-handler app-state msg))))
        (is (= (get-in initial-app-state [:user :current-team :team/id])
               (-> app-state deref :user :current-team :team/id)))))))

(deftest spa-navigate-search
  (let [empty-app-state {:cache {}}
        small-app-state {:cache {:foo {:datum/label "the people talk"
                                       :db/id "tempid1"}
                                 :bar {:literal/math "x_y = 4 - 7^24^y"
                                       :db/id 42}
                                 :baz {:fact/attribute "has"
                                       :fact/value "corned beef hash"
                                       :db/id "six"}}}]
    (testing "empty cache"
      (let [app-state (atom empty-app-state)
            [query-string entity-type :as query] ["*" nil]
            msg (msg/search query-string entity-type)]
        (is (= [(msg/navigate-search-success query-string entity-type [])]
               (:messages (local-request-handler app-state msg))))))
    (testing "empty query string"
      (let [app-state (atom small-app-state)
            [query-string entity-type] ["" nil]
            msg (msg/search query-string entity-type)]
        (is (= [(msg/navigate-search-success query-string entity-type [])]
               (:messages (local-request-handler app-state msg))))))
    (testing "exact match datum"
      (let [app-state (atom small-app-state)
            [query-string entity-type] ["the people talk" nil]
            msg (msg/search query-string entity-type)]
        (is (= [(msg/navigate-search-success
                 query-string entity-type
                 [(1 {:datum/label "the people talk" :db/id "tempid1"})])]
               (:messages (local-request-handler app-state msg))))))
    ;; FIXME: failing. something to do with regex with that formula? works when
    ;; value is `foobar'
    (testing "exact match literal"
      (doseq [literal-type (keys schema/literal-types)]
        (let [value "x_y = 4 - 7^24^y" ;; "foobar"
              app-state (atom (assoc-in small-app-state [:cache :bar]
                                        {literal-type value :db/id 42}))
              [query-string entity-type] [value nil]
              msg (msg/search query-string entity-type)]
          (is (= [(msg/navigate-search-success
                   query-string entity-type
                   [(list 1 {literal-type value :db/id 42})])]
                 (:messages (local-request-handler app-state msg)))))))
    ;; FIXME: failing
    (testing "exact match fact"
      (let [app-state (atom small-app-state)
            [query-string entity-type] ["corned beef hash" nil]
            msg (msg/search query-string entity-type)]
        (is (= [(msg/navigate-search-success
                 query-string entity-type
                 [(list 1 {})])]
               (:messages (local-request-handler app-state msg))))))
    (testing "entity type filter datum"
      (let [app-state (atom small-app-state)
            [query-string entity-type] [nil :datum]
            msg (msg/search query-string entity-type)]
        (is (= [(msg/navigate-search-success query-string entity-type
                                             [(list )])]
               (:messages (local-request-handler app-state msg))))))
    (testing "entity type filter literal"
      (let [app-state (atom small-app-state)
            [query-string entity-type] [nil :literal]]
        ))
    (testing "entity type filter fact"
      (let [app-state (atom small-app-state)
            [query-string entity-type] [nil :fact]]
        ))
    (testing "sort by match quality")
    (testing "case insensitive")
    (testing "optionally limit number of results")
    (testing "unique results")))
