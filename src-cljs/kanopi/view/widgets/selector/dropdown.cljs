(ns kanopi.view.widgets.selector.dropdown
  "Generic dropdown widget."
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.util.browser :as browser]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.aether.core :as aether]
            [cljs.core.async :as async]
            [cljs-uuid-utils.core :as uuid]
            [cljs-time.core :as t]))

(declare open-dropdown!)

;; TODO: refactor into a re-usable timer toy thing
(defn- start-hover! [owner]
  (let [kill-hover-clock (async/chan 1)]
    (om/set-state! owner ::kill-hover-clock-ch kill-hover-clock)
    (asyncm/go
     (let [[v ch] (async/alts! [kill-hover-clock (async/timeout 1000)])]
       ;; NOTE: non-nil value must be sent to kill-channel
       (when (nil? v)
         (open-dropdown! owner))
       (async/close! kill-hover-clock)))
    nil))

(defn- stop-hover! [owner]
  (async/put! (om/get-state owner ::kill-hover-clock-ch) :stop!)
  nil)

(defn- toggle-dropdown! [owner]
  (om/update-state! owner :expanded not)
  (stop-hover! owner))

(defn- open-dropdown! [owner]
  (om/set-state! owner :expanded true)
  (stop-hover! owner))

(defn- close-dropdown! [owner]
  (om/set-state! owner :expanded false)
  (stop-hover! owner))

(defmulti dropdown-menu-item (fn [_ _ itm] (:type itm)))

(defmethod dropdown-menu-item :link
  [owner idx itm]
  [:li.dropdown-menu-item
   {:react-key idx}
   [:a {:on-click (juxt (get itm :on-click (constantly nil))
                        #(close-dropdown! owner))
        :href (get itm :href)}
    [:span (get itm :label)]]])

(defmethod dropdown-menu-item :divider
  [owner idx itm]
  [:li.divider {:role "separator", :react-key idx}])

(defn dropdown-menu [owner]
  ;; FIXME: do something different when there's only 1 menu
  ;; item. no reason to let user expand menu.
  (let [{:keys [expanded selection-handler menu-items]} (om/get-state owner)]
    (into
     [:ul.dropdown-menu
      {:style          {:display (when expanded "inherit")}
       :on-click       selection-handler
       :on-mouse-enter #(open-dropdown! owner)
       :on-mouse-leave #(close-dropdown! owner)}]
     (map-indexed (partial dropdown-menu-item owner) menu-items))))

(defn link-dropdown [owner]
  (let [{:keys [tab-index expanded toggle-icon-fn caret? toggle-label classes]}
        (om/get-state owner)]
    [:li.dropdown
     {:class (concat [] classes)}
     [:a.dropdown-toggle
      {:on-click       #(toggle-dropdown! owner)
       :data-toggle    "dropdown"
       :tab-index      tab-index
       :role           "button"
       :aria-haspopup  "true"
       :aria-expanded  expanded
       :on-mouse-enter #(start-hover! owner)
       :on-mouse-leave #(close-dropdown! owner)}
      (cond
       toggle-icon-fn
       [:span (toggle-icon-fn)
        (when caret?
          [:span.caret])]

       toggle-label
       [:span (str toggle-label " ")
        (when caret?
          [:span.caret])]
       )]
     (dropdown-menu owner)]))

(defn split-button-dropdown [owner]
  (let [{:keys [tab-index expanded toggle-label button-on-click]}
        (om/get-state owner)]
    [:div.btn-group
     {:on-mouse-enter #(start-hover! owner)
      :on-mouse-leave #(close-dropdown! owner)}
     [:button.btn
      {:type "button"
       :on-click button-on-click}
      toggle-label]

     [:button.btn.dropdown-toggle
      {:data-toggle "dropdown"
       :aria-haspopup "true"
       :aria-expanded expanded
       :on-click #(toggle-dropdown! owner)
       :tab-index tab-index
       ; :on-mouse-enter #(start-hover! owner)
       ; :on-mouse-leave #(close-dropdown! owner)
       }
      [:span.caret]
      [:span.sr-only
       "Toggle Dropdown"]]

     (dropdown-menu owner)]))

(defn dropdown
  "All configuration is passed via local state."
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      ;; defaults
      {
       :toggle-type :link
       :toggle-label "Toggle label"
       ;; For current value with split-button-dropdown.
       :button-on-click (constantly nil)
       :selection-handler (constantly nil)
       :tab-index 0
       ;; TODO: caret-fn which takes dropdown state
       :menu-items [{:type :link
                     :href ""
                     :label "Item 1"}
                    {:type :divider}
                    {:type :link
                     :on-click (constantly nil)
                     :label "Item 2"}]
       :expanded nil
       ;; internal stuff
       ::when-hover-started nil})

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         (case (get state :toggle-type)
           :link
           (link-dropdown owner)

           :split-button
           (split-button-dropdown owner)

           ; default
           (link-dropdown owner))
         )))))

