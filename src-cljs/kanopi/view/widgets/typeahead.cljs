(ns kanopi.view.widgets.typeahead
  "What do I want from typeahead search?
  
  "
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [cljs.core.async :as async]
            [kanopi.model.message :as msg]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.util.async :as async-util]
            [kanopi.ether.core :as ether]))

(defn typeahead
  "Pre-load with local or cached results.
  "
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:input-ch (-> (async/chan)
                     (async-util/debounce 250)
                     (async/pipe (om/get-shared owner [:ether :publisher])))
       :selection-index 0
       })

    om/IRenderState
    (render-state [_ {:keys [focused input-ch input-value] :as state}]
      (let [search-results
            (get ((om/get-shared owner :search-results)) input-value []) 
            ]
        (html
         [:div.typeahead
          [:input
           {:type        "text"
            :on-focus    #(om/set-state! owner :focused true)
            :on-blur     #(om/set-state! owner :focused false)
            :value       (get state :input-value)
            :on-change   #(let [v (.. % -target -value)]
                          (async/put! input-ch (msg/search v))
                          (om/set-state! owner :input-value v)) 
            :on-key-down #(case (.-key %)
                            "ArrowDown"
                            (do (om/update-state! owner :selection-index
                                                  (fn [x]
                                                    (if (< x (dec (count search-results)))
                                                      (inc x)
                                                      x)))
                                (. % preventDefault))
                            "ArrowUp"
                            (do (om/update-state! owner :selection-index
                                                  (fn [x]
                                                    (if (> x 0)
                                                      (dec x)
                                                      x)))
                                (. % preventDefault))
                            "Enter"
                            (do
                             (info "enter!" (nth search-results (get state :selection-index)))
                             ;; TODO: select the value! whatever that
                             ;; means.
                             )
                            nil)
            }]
          [:ul.dropdown-menu.typeahead-results
           {:style {:display (when (and focused (not-empty search-results))
                               "inherit")}}
           (for [[idx res] (map-indexed vector search-results)]
             [:li
              [:a {:style {:font-weight (when (= idx (get state :selection-index)) "500")} }
               [:span (str res)]]])]
          ])))))
