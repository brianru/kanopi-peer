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
            [kanopi.model.schema :as schema]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.util.browser :as browser]
            [kanopi.util.async :as async-util]
            [kanopi.ether.core :as ether]))

(defn- handle-key-down
  [owner evt search-results]
  (case (.-key evt)
    "ArrowDown"
    (do (om/update-state! owner :selection-index
                          (fn [x]
                            (if (< x (dec (count search-results)))
                              (inc x)
                              x)))
        (. evt preventDefault))
    "ArrowUp"
    (do (om/update-state! owner :selection-index
                          (fn [x]
                            (if (> x 0)
                              (dec x)
                              x)))
        (. evt preventDefault))
    "Enter"
    (let [[_ selected-result] (nth search-results (om/get-state owner :selection-index))]
      (println "here" selected-result)
      (om/update-state! owner
                        (fn [state]
                          (assoc state :focused false
                                 :input-value (schema/get-value selected-result))))
      (when-let [href-fn (om/get-state owner :href-fn)]
        (println "here")
        (browser/set-page! owner (href-fn selected-result))))
    "Escape"
    (do
     (om/set-state! owner :focused false))

    ;; default
    nil))

(defn- handle-result-click
  [owner res evt]
  (om/update-state! owner
                    (fn [state]
                      (assoc state :focused false
                             :input-value (schema/get-value res)))))

(defn- element-specific-attrs
  [{:keys [element-type] :as state}]
  (case element-type
    :input
    (hash-map :type "text")

    :textarea
    (hash-map :cols 32, :rows 3)

    ;; default
    {}))

(defn typeahead
  "Pre-load with local or cached results.

  Supports different element types.
  "
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      ;; FIXME: debounce is not working
      {:element-type :input ;; supported values are #{:input :textarea}
       :input-ch (-> (async/chan)
                     (async-util/debounce 100)
                     (async/pipe (om/get-shared owner [:ether :publisher])))
       :display-fn schema/display-entity
       :on-click (constantly nil)
       :href-fn (constantly nil)
       :selection-index 0
       :input-value nil
       })

    om/IRenderState
    (render-state [_ {:keys [focused input-ch input-value display-fn] :as state}]
      (let [search-results
            (get ((om/get-shared owner :search-results)) input-value []) 
            ]
        (html
         [:div.typeahead
          (vector
           ;; NOTE: this is insane. It may be better to have separate
           ;; blocks for each element type, though that would lead to
           ;; more duplicated code.
           (get state :element-type)
           (merge (element-specific-attrs state)
                  {:on-focus    #(om/set-state! owner :focused true)
                   ;:on-blur     #(om/set-state! owner :focused false)
                   :value       (get state :input-value)
                   :on-change   #(let [v (.. % -target -value)]
                                   (async/put! input-ch (msg/search v))
                                   (om/set-state! owner :input-value v)) 
                   :on-key-down #(handle-key-down owner % search-results)
                   }))
          [:ul.dropdown-menu.typeahead-results
           {:style {:display (when (and focused (not-empty search-results))
                               "inherit")}}
           (for [[idx [score res]] (map-indexed vector search-results)]
             [:li.dropdown-menu-item
              (when (= idx (get state :selection-index))
                [:span.dropdown-menu-item-marker])
              [:a {
                   ;;:style    {:font-weight (when (= idx (get state :selection-index))
                   ;;                          "500")}
                   :href     ((get state :href-fn) res)
                   :on-click (juxt (partial (get state :on-click) res)
                                   (partial handle-result-click owner res))
                   }
               [:span (display-fn res)]]])]
          ])))))
