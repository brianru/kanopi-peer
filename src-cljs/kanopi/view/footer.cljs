(ns kanopi.view.footer
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(defn footer
  "
  "
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "footer")
    
    om/IRender
    (render [_]
      (let []
        (html
         [:footer
          [:div.container-fluid
           (when true ;;debug
             [:div.row
              [:div.col-md-offset-2.col-md-2
               [:label "Mode:"]
               [:div (str (get props :mode))]
               ]
              [:div.col-md-2
               [:label "Aether"]
               ]
              [:div.col-md-2
               [:label "User"]
               [:table
                [:tbody
                 (for [[k v] (get props :user)]
                   [:tr {:key k}
                    [:td (str k)]
                    [:td (str v)]])]
                ]]
              [:div.col-md-2
               [:label "Other stuff"]]])
           [:div.row
            [:div.col-md-offset-2.col-md-2
             [:label "About"]]
            [:div.col-md-2
             [:label "Help"]]
            [:div.col-md-2
             [:label "Contact"]]
            ]
           ]
          ])))))

