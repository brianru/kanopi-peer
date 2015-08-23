(ns kanopi.model.state
  )

(def app-state
  (atom {:tempo {:pulse nil}
         :user  {:actions {}}
         :data  {}}))
