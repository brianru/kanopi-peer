(ns kanopi.view.icons
  (:require [kanopi.util.browser :as browser]))

(defn open [m]
  [:div.glyphicon.glyphicon-new-window
   (merge {} m)])

(defn edit-in-place []
  )

(defn link-to
  "Example usage:
  (->> (icons/open {})
       (icons/link-to owner :settings))
  "
  [owner route icon-markup]
  (let [route (if (sequential? route) route (vector route))]
    [:a {:href (apply browser/route-for owner route)}
     icon-markup]))
