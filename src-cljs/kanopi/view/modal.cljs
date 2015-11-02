(ns kanopi.view.modal
  (:require [om.core :as om]))

(defn modal-template [spec]
  [:div.modal
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:h4.modal-title (get spec :title)]]
     [:div.modal-body
      ]
     [:div.modal-footer
      ]]]])
