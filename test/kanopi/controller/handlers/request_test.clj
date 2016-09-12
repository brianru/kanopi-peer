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
  (let [app-state (atom {:user {:teams [{:team/id "abc"}
                                        {:team/id "456"}
                                        {:team/id "team1"}]}})]
    (testing ""
      (let [msg (msg/switch-team "not a team")]
        (is
         (= [(msg/switch-team-success (-> app-state deref :user))]
            (:messages (local-request-handler app-state msg))))))))
