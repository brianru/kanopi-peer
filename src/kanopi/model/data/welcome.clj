(ns kanopi.model.data.welcome)

(def data
  [

   {:db/id #db/id [:db.part/structure -3000]
    :datum/label "Welcome to Kanopi"
    :datum/fact [#db/id [:db.part/structure -3001]

                 
                 ]}
   {:db/id #db/id [:db.part/structure -3001]
    }
   
   ])

(defn welcome-data
  [creds]
  data)
