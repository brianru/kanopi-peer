(ns kanopi.view.icons
  (:require [kanopi.util.browser :as browser]))

(defn open [m]
  [:div.glyphicon.glyphicon-new-window
   (merge {} m)])

(defn edit-in-place [m]
  [:div.glyphicon.glyphicon-pencil
   (merge {} m)])

(defn log-in [m]
  [:div.glyphicon.glyphicon-log-in
   (merge {} m)])

(defn link-to
  "Example usage:
  (->> (icons/open {})
       (icons/link-to owner :settings {:class \"navbar-brand\"}))
  "
  ([owner route icon-markup]
   (link-to owner route {} icon-markup))

  ([owner route m icon-markup]
   (let [route (if (sequential? route) route (vector route))]
    [:a (merge m {:href (apply browser/route-for owner route)}) 
     icon-markup])))
