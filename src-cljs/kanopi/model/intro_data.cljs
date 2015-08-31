(ns kanopi.model.intro-data)

(def intro-data
  {-1000 {:db/id -1000
          :thunk/label "Welcome to Kanopi!"
          :thunk/fact #{-1001 -1002 -1007 -1015}}
   -1001 {:db/id -1001
          :fact/attribute #{-1003}
          :fact/value     #{-1004}}
   -1003 {:db/id -1003
          :thunk/label "Slogan"}
   -1004 {:db/id -1004
          :thunk/label "A new way to explore and document your world. Express information with the structure you want without losing the flexibility you need."}

   -1002 {:db/id -1002
          :fact/attribute #{-1005}
          :fact/value     #{-1006}}
   -1005 {:db/id -1005
          :thunk/label "Created by"}
   -1006 {:db/id -1006
          :thunk/label "Brian James Rubinton"}

   -1007 {:db/id -1007
          :fact/attribute #{-1008}
          :fact/value     #{-1009}}
   -1008 {:db/id -1008
          :thunk/label "How it works"}
   -1009 {:db/id -1009
          :thunk/label "Click here to find out!"
          }

   ;; Examples
   -1011 {:db/id -1011
          :thunk/label "Example"}

   -1010 {:db/id -1010
          :fact/attribute #{-1011}
          :fact/value     #{-1012}}
   -1012 {:db/id -1012
          :thunk/label "Infinite Jest by David Foster Wallace"}

   -1013 {:db/id -1013
          :fact/attribute #{-1011}
          :fact/value     #{-1014}}
   -1014 {:db/id -1014
          :thunk/label "August 17, 2015 Workout"}

   -1015 {:db/id -1015
          :fact/attribute #{-1011}
          :fact/value     #{-1016}}
   -1016 {:db/id -1016
          :thunk/label "Kanopi Pattern Language"
          :thunk/fact #{-1017 -1018}}

   -1019 {:db/id -1019
          :thunk/label "Pattern"}
   -1017 {:db/id -1017
          :fact/attribute #{-1019}
          :fact/value     #{-1020}}
   -1020 {:db/id -1020
          :thunk/label "Lantern in the fog"}
   -1018 {:db/id -1018
          :fact/attribute #{-1019}
          :fact/value     #{-1021}}
   -1021 {:db/id -1021
          :thunk/label "Frictionless expansion and exploration of thoughts."}

   })
