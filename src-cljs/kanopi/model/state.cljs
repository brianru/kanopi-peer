(ns kanopi.model.state
  (:require [shodan.console :refer-macros (log) :include-macros true]))

(def app-state
  (atom {:tempo {:pulse nil}
         :user  {:actions {}}
         :data  {}}))
