(ns kanopi.model.schema-test
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [kanopi.model.schema :as schema]
            [kanopi.main :refer [default-config]]
            [clojure.java.io :as io]))


(deftest all-literals-are-covered
  (let [db-schema (-> default-config (get-in [:datomic :schema])
                      first io/resource slurp read-string)
        db-literal-idents (-> db-schema
                              (->> (map :db/ident)
                                   (filter #(= "literal" (namespace %))))
                              set
                              (disj :literal/representation :literal/team))
        schema-literal-idents (set (keys schema/literal-types)) ]
    (is (= db-literal-idents schema-literal-idents))))
