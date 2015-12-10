(ns kanopi.view.widgets.modal
  (:require [om.core :as om]))

(defn modal-template [{:keys [title body]}]
  [:div.modal
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:h4.modal-title title]]
     [:div.modal-body
      body]
     [:div.modal-footer
      ]]]])
