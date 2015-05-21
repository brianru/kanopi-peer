(ns nebula.generators
  (:require [clojure.test.check.generators :as gen]
            ))

(def user-gen
  (apply hash-map
         (interleave [])))
