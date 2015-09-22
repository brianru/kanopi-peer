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
               [:div (name (get props :mode))]
               [:div
                (when (= :demo (get props :mode))
                  [:button {:on-click #(om/update! props :mode :authenticated)}
                   "Authenticate!"])
                (when (= :authenticated (get props :mode))
                  [:button {:on-click #(om/update! props :mode :demo)}
                   "Demo Mode!"])
                ]]
              [:div.col-md-2
               [:label "Aether"]
               ]
              [:div.col-md-2
               [:label "User"]]
              [:div.col-md-2
               [:label "Other stuff"]]])
           [:div.row
            [:div.col-md-offset-2.col-md-2
             [:label "About us"]]]
           ]
          ])))))

