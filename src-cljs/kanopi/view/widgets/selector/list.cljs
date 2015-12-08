(ns kanopi.view.widgets.selector.list
  (:require [om.core :as om]
            [sablono.core :refer-macros (html)]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(defn vertical-menu [heading current-item items on-click]
  (let []
    (apply conj
           [:nav.menu]
           [:h3.menu-heading heading]
           (for [{id :ident :as itm} items]
             [:a.menu-item
              {:class [(when (= id current-item)
                         "selected")]
               :on-click #(on-click id)}
              (get itm :label)]))))
