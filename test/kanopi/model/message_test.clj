(ns kanopi.model.message-test
  (:require [clojure.test :refer :all]
            schema.test
            [schema.core :as s]
            [schema.experimental.generators :as schema-gen]
            
            [kanopi.model.message :as msg]
            ))

(use-fixtures :once schema.test/validate-schemas)

(deftest play
  (let []
    (is (nil? (schema-gen/generate msg/UpdateFact)))))
