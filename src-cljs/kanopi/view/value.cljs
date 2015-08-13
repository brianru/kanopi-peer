(ns kanopi.view.value
  "Within each granularity component, do a pattern match on value type
  to determine the correct underlying component."
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [shodan.console :refer-macros [log] :include-macros true]
            ))

(defn hover-image [])
(defn thumbnail [])
(defn link [])
(defn text [])
(defn interactive-map [])

(defn stub-rendering
  [props owner opts])

(defn partial-rendering
  [props owner opts])

(defn full-rendering
  [props owner opts])
