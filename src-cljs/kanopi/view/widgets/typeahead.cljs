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
  [owner search-results evt]
 
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
      (om/update-state! owner
                        (fn [state]
                          (assoc state :focused false
                                 :input-value (schema/get-value selected-result))))
      ;; href-fn always exists, but should only be used when it
      ;; produces a truthy value. by default it always evaluates to
      ;; nil.
      (when-let [href ((om/get-state owner :href-fn) selected-result) ]
        (browser/set-page! owner href))

      ;; on-click fn is side-effecting, so it may always be called
      ;; even if its default is used, which always evaluates to nil
      ((om/get-state owner :on-click) selected-result evt)

      (.. evt -target (blur)))

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

(defn handle-typeahead-input [owner evt]
  (let [v (.. evt -target -value)]
    (async/put! (om/get-state owner :input-ch) (msg/search v))
    ((om/get-state owner :on-change) v)
    (om/set-state! owner :input-value v)))

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
                     (async/pipe (msg/publisher owner)))
       :display-fn schema/display-entity

       :on-change (constantly nil)

       ;; NOTE: on-click is side-effecting
       :on-click (constantly nil)
       ;; NOTE: href-fn is a pure function which returns a url path
       :href-fn (constantly nil)
       ;; SUMMARY: these 2 are different because their usage in web
       ;; browsers is different. we're just sticking to that. when
       ;; href's value is nil the browser ignores it. 

       :selection-index 0
       :input-value nil
       })

    om/IRenderState
    (render-state [_ {:keys [focused input-ch input-value display-fn on-change]
                      :as state}]
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
                   :on-change   (partial handle-typeahead-input owner) 
                   :on-key-down (partial handle-key-down owner search-results)
                   }))
          [:ul.dropdown-menu.typeahead-results
           {:style {:display (when (and focused (not-empty search-results))
                               "inherit")}}
           (for [[idx [score res]] (map-indexed vector search-results)]
             [:li.dropdown-menu-item
              (when (= idx (get state :selection-index))
                [:span.dropdown-menu-item-marker])
              [:a {
                   :href     ((get state :href-fn) res)
                   :on-click (juxt (partial (get state :on-click) res)
                                   (partial handle-result-click owner res))
                   }
               [:span (display-fn res)]]])]
          ])))))
