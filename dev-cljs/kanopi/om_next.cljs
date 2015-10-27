(ns kanopi.om-next
  (:require-macros [devcards.core :refer (defcard deftest)])
  (:require [goog.dom :as gdom]
            [sablono.core :as sab]
            [om.next :as om :refer-macros (defui)]
            [om.dom :as dom]))

(defui Hello
  Object
  (render [this]
    (dom/p nil (-> this om/props :text))))

(def hello (om/factory Hello))

(defcard simple-component
  "Test that Om Next component work as regular React components."
  (hello {:text "Hello, dev world!"}))

(defcard another-card
  (sab/html [:h1 "BBBBBB"]))
