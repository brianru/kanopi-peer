(ns kanopi.generators
  "I don't want to generate sample data.

  I want to generate simulation data. I want to input all possible
  values into the system that can get into the system and see what
  happens."
  (:require [clojure.test.check.generators :as gen]
            ))


(defn mk-register-gen [known-usernames]
  (gen/hash-map
   :user/username (gen/frequency [[5 gen/string-ascii]
                                  [5 (gen/elements known-usernames)]])
   :user/password gen/string-ascii))

(defn mk-login-gen [known-passwords]
  (gen/hash-map
   :user/username gen/string-ascii
   :user/password (gen/frequency [[5 gen/string-ascii]
                                  [5 (gen/elements known-passwords)]
                                  ])
   ))
