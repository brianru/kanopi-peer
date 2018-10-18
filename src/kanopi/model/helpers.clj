(ns kanopi.model.helpers
  (:require [kanopi.model.schema :as schema]
            [schema.core :as s #?@(:cljs [:include-macros true])]
            ))

(defonce temp-id
  (atom -1))

(defn new-temp-id
  "TODO: at some point this will overflow, I should probably be
  checking and resetting past some absurdly high number."
  []
  (swap! temp-id dec))

(defn init-datum
  ([creds]
   (init-datum creds (new-temp-id)))

  ([creds temp-id]
   (hash-map
    :db/id       temp-id
    :datum/team  (schema/current-team creds)
    :datum/label ""
    :datum/fact  [])))

(defn init-fact
  ([creds]
   (init-fact creds (new-temp-id)))

  ([creds temp-id]
   (hash-map
    :db/id temp-id
    :datum/team     (schema/current-team creds)
    :fact/attribute nil
    :fact/value     nil)))

(defn init-literal
  ([creds]
   (init-literal creds (new-temp-id)))

  ([creds temp-id]
   (hash-map
    :db/id        temp-id
    :literal/team (schema/current-team creds))))

