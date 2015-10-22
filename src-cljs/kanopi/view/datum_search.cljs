(ns kanopi.view.datum-search
  (:require [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [om.core :as om]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.model.ref-cursors :as ref-cursors]
            [kanopi.util.browser :as browser]
            ))

(defn datum-list-entry [owner [db/id lbl _ _]]
  [:div
   [:a {:href (when id
                (browser/route-for owner :datum :id id))}
    [:span lbl]]])

(defn most-viewed-datums [owner datums]
  [:div.panel.panel-default
   [:div.panel-heading
    [:h3.panel-title "Most Viewed"]]
   (into [:div.panel-body]
         (map datum-list-entry datums)) 
   ])

(defn most-edited-datums [owner datums]
  [:div.panel.panel-default
   [:div.panel-heading
    [:h3.panel-title "Most Edited"]]
   (into [:div.panel-body]
         (map datum-list-entry datums))
   ])

(defn recently-touched-datums [owner datums]
  [:div.panel.panel-default
   [:div.panel-heading
    [:h3.panel-title "Recent"]]
   (into [:div.panel-body]
         (map datum-list-entry datums))
   ])

(defn suggestions
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "datum-search-suggestions")
    om/IRender
    (render [_]
      (let [recent-datums (if (> (count (get props :recent-datums)) 3)
                            (get props :recent-datums)
                            (concat (get props :recent-datums)
                                    (map #(vector (:db/id %) (:datum/label %) nil nil)
                                         (:cache props))))
            ]
        (html
         [:div.container-fluid
          [:div.row
           [:div.col-md-4
            (most-viewed-datums owner (get props :most-viewed-datums))]
           [:div.col-md-4
            (most-edited-datums owner (get props :most-edited-datums))]
           [:div.col-md-4
            (recently-touched-datums owner recent-datums)]]
          [:div.row
           [:h3 "Datum finder"]
           [:span "TODO"]
           ]
          ])))))
