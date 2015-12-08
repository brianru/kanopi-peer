(ns kanopi.view.widgets.panel)

(defn default [heading content]
  [:div.panel.panel-default
   [:div.panel-heading
    [:h2.panel-title heading]]
   [:div.panel-body content]])
