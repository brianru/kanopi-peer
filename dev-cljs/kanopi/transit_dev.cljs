(ns kanopi.transit-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [com.stuartsierra.component :as component]
            [cljs.test :refer-macros (testing is)]
            [cognitect.transit :as t]))

(defn roundtrip [x]
  (let [w (t/writer :json)
        r (t/reader :json)]
    (t/read r (t/write w x))))

(deftest spout-messages
  (let [msg 
        {:params {:verb :datum.label/update
                  :tx/id "f522b994-bac9-42b5-85d4-bfb294e662bd"
                  :context {}
                  :noun {:existing-entity {:db/id 281474976711726
                                           :datum/label ""
                                           :datum/team {:db/id 277076930200587
                                                        :team/id "brian"}}
                         :new-label "new label please"}}
         :format :transit
         :response-format :transit}
        msg-with-fns (assoc msg :handler str)
        ]
    (is (= msg (roundtrip msg)))
    (is (= msg-with-fns (roundtrip msg-with-fns)))))
