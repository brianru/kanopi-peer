(ns kanopi.controller.handlers.request-test
  "Test methods for kanopi.controller.handlers.request/local-request-handler"
  (:require [clojure.test :refer :all]
            [kanopi.controller.handlers.request :refer :all]
            [kanopi.model.message :as msg]))

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
  (testing "empty cache")
  (testing "sort by match quality")
  (testing "case insensitive")
  (testing "optionally limit number of results")
  (testing "empty query string")
  (testing "unique results")
  (let [app-state (atom {:cache {}})]))
