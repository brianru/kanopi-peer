(ns kanopi.view.widgets.typeahead
  "What do I want from typeahead search?
  
  "
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [cljs.core.async :as async]
            [goog.dom :as gdom]
            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.util.browser :as browser]
            [kanopi.util.async :as async-util]
            [kanopi.util.core :as util]
            [kanopi.aether.core :as aether]))

(defn- handle-result-click
  [owner res evt]
  (om/update-state!
   owner
   (fn [state]
     (assoc state
            :focused false
            :input-value (schema/get-value res))))
  (if-let [href-fn (om/get-state owner :href-fn)]
    (browser/set-page! owner (href-fn res))
    ((om/get-state owner :on-click) res evt)))

(defn- handle-submission [owner evt]
  (let [{submit-fn :on-submit value :input-value}
        (om/get-state owner)]
    (submit-fn value evt)
    (om/set-state! owner :focused false)))

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

    ("Enter" "Tab")
    (let [[idx selected-result]
          (nth search-results (om/get-state owner :selection-index) nil)
          ]
      (. evt preventDefault)

      (if selected-result
        (do
         ; NOTE: this should only happen when user is not editing the typeahead
         (handle-result-click owner selected-result evt) 
         ;; href-fn always exists, but should only be used when it
         ;; produces a truthy value. by default it always evaluates to
         ;; nil.
         (when-let [href ((om/get-state owner :href-fn) selected-result) ]
           (browser/set-page! owner href))

         ;; on-click fn is side-effecting, so it may always be called
         ;; even if its default is used, which always evaluates to nil
         ((om/get-state owner :on-click) selected-result evt))
        (do
         (handle-submission owner evt)) )

      (.. evt -target (blur)))

    "Escape"
    (do
     (. evt preventDefault)
     (om/set-state! owner :focused false)
     (om/update-state! owner #(assoc % :input-value (:initial-input-value %)))
     (.. evt -target (blur)))

    ;; default
    nil))


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

  NOTE: everything this component does besides take input and return
  matching search reuslts is configurable by passing functions in as
  initial state.
  "
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:element-type :input ;; supported values are #{:input :textarea}
       :tab-index 0 ;; decided by platform convention by default

       ;; Used to render search results into strings for display.
       :display-fn str
       ;; What to display when there are no search results.

       ;; FIXME: use data of the right shape here.
       :empty-result [[nil "No matching entities. Hit Enter to create one."]]

       ;; Escape hatch for tracking the input field's value.
       :on-change (constantly nil)

       ;; Outside world can track this too!
       ;; Fns take 1 arg: the current input value.
       :on-focus (constantly nil)
       :on-blur  (constantly nil)
       ;; Submission is different from selection (below).
       :on-submit (constantly nil)

       ;; The Selection Handlers.
       ;; NOTE: on-click is side-effecting
       :on-click (constantly nil)
       ;; NOTE: href-fn is a pure function which returns a url path
       :href-fn (constantly nil)
       ;; SUMMARY: these 2 are different because their usage in web
       ;; browsers is different. we're just sticking to that. when
       ;; href's value is nil the browser ignores it. 

       ;; INTERNAL
       ;; TODO: use namespaced keywords for internal stuff
       ;; FIXME: debounce is not working...or is it?
       :input-ch (-> (async/chan)
                     (async-util/debounce 100)
                     (async/pipe (msg/publisher owner)))
       :selection-index -1
       :focused false
       ; initial-input-value should be set externally
       :initial-input-value nil
       ; input-value should be kept internal
       :input-value nil
       :placeholder "Placeholder"
       })

    ;; NOTE: this plus the stopPropagation beyond the input element
    ;; below are an extremely ugly hack to make it so clicking outside
    ;; the typeahead removes focus, while clicking inside works
    ;; without causing the field to blur.
    ;;
    ;; The problem: clicking items in the results dropdown menu.
    ;; Those items would cause the input field to blur, in blur we'd
    ;; remove focus from the input field, and then the click would not
    ;; propagate. Blurs just don't propagate other events. It sucks.
    ;;
    ;; TODO: refactor with closure event lib
    ;; FUCK: this breaks on page changes because the header unmounts
    ;; and remounts, but not always in the right order or owner
    ;; somehow changes.
    ;; Terrible solution: set window.onclick on every render :(
    om/IWillUnmount
    (will-unmount [_]
      (set! js/window.onclick nil))

    om/IRenderState
    (render-state [_ {:keys [focused input-ch input-value display-fn
                             on-focus on-blur on-change href-fn
                             ]
                      :as state}]
      ;; NOTE: see NOTE above om/IWillMount for explanation
      (set! js/window.onclick #(om/set-state! owner :focused false))
      (let [all-search-results (om/observe owner ((om/get-shared owner :search-results)))
            search-results (get all-search-results input-value (get state :empty-result []))
            ]
        (html
         [:div.typeahead
          ;; NOTE: see NOTE above om/IWillMount for explanation
          {:on-click (fn [evt]
                       (. evt stopPropagation))}
          (vector
           ;; NOTE: this is insane. It may be better to have separate
           ;; blocks for each element type, though that would lead to
           ;; more duplicated code.
           (get state :element-type)
           (merge (element-specific-attrs state)
                  {:on-focus    (fn [_]
                                  (om/set-state! owner :focused true)
                                  (on-focus (get state :input-value))) 
                   :tab-index (get state :tab-index)
                   :value       (or (get state :input-value)
                                    (get state :initial-input-value))
                   :placeholder (get state :placeholder)
                   :style {:border-bottom-color
                           (when (om/get-state owner :focused) "green")
                           }
                   :on-change   (partial handle-typeahead-input owner) 
                   :on-key-down (partial handle-key-down owner search-results)
                   }))
          [:ul.dropdown-menu.typeahead-results
           {:style {:display (if (and (om/get-state owner :focused)
                                      (not-empty input-value)
                                      (not-empty search-results))
                               "inherit"
                               "none")}}
           (for [[idx [score res]] (map-indexed vector search-results)]
             [:li.dropdown-menu-item
              (when (= idx (get state :selection-index))
                [:span.dropdown-menu-item-marker])
              [:a {:on-click (partial handle-result-click owner res)}
               ; (if href-fn
               ;   {:href (href-fn res)}
               ;   {:on-click (partial handle-result-click owner res)})
               [:span (display-fn res)]]])]
          ])))))
