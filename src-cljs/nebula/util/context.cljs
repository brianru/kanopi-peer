(ns nebula.util.context
  (:require [om.core :as om]))

(defn visible?
  "How visible is the given component?
  :complete
  :partial
  nil"
  [owner]
  nil)

(defn location
  "Where is the component located on the screen?
  [<vertical> <horizontal>]
  [#{:top :center :bottom nil} #{:left :center :right nil}]"
  [owner]
  nil)

(defn position
  "Is this fn necessary or should consumers do this themselves?"
  [owner]
  ((juxt visible? location) owner))
