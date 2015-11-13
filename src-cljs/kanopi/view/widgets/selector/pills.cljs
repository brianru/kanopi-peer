(ns kanopi.view.widgets.selector.pills
  (:require [om.core :as om]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(defn generate-item [state itm]
  [:li
   [:a.horizontal-selector-item
    {:on-click (get itm :on-click) 
     :class [(if (= (get state :current-item) (get itm :ident))
               "selected")]
     }
    [:span (get itm :label "")]] ])

(defn horizontal
  ""
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {
       :identity-fn identity
       :display-fn str

       :start-idx 1

       :menu-items [{:type :link
                     :href ""
                     :label "Item 1"}
                    {:type :divider}
                    {:type :link
                     :on-click (constantly nil)
                     :label "Item 2"}]
       })

    om/IRenderState
    (render-state [_ {:keys [menu-items start-idx] :as state}]
      (let [
            ;; TODO: make this smart!
            number-items-to-display 3
            end-idx (dec (+ number-items-to-display start-idx))
            ]
        (html
         [:ul.pager.horizontal-selector
           (when (or true (> (count menu-items) number-items-to-display))
             [:li.previous
              {:class [(when-not (> 1 start-idx) "disabled")]}
              [:a.horizontal-selector-item
               [:span \u2190]] ])

           ;; TODO: animate these in and out
           (map (partial generate-item state) menu-items)

           (when (or true (> (count menu-items) number-items-to-display))
             [:li.next
              {:class [(when (< (count menu-items) end-idx) "disabled")]}
              [:a.horizontal-selector-item
               [:span \u2192]]
              ])
           ]
         )))))
