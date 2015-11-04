(ns kanopi.view.widgets.dropdown
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

;; NOTE: I'm aware there's no `open-dropdown!`. I don't know if we
;; need one.
(defn- toggle-dropdown! [owner]
  (om/update-state! owner :expanded not))
(defn- open-dropdown! [owner]
  (om/set-state! owner :expanded true))
(defn- close-dropdown! [owner]
  (om/set-state! owner :expanded false))

;; TODO: refactor into a re-usable timer toy thing
(defn- start-hover! [owner]
  (let [kill-hover-clock (async/chan 1)]
    (om/set-state! owner ::kill-hover-clock-ch kill-hover-clock)
    (asyncm/go
     (let [[v ch] (async/alts! [kill-hover-clock (async/timeout 1000)])]
       ;; NOTE: non-nil value must be sent to kill-channel
       (when (nil? v)
         (open-dropdown! owner))
       (async/close! kill-hover-clock)))))

(defn- stop-hover! [owner]
  (async/put! (om/get-state owner ::kill-hover-clock-ch) :stop!))

(defmulti dropdown-menu-item (fn [_ _ itm] (:type itm)))

(defmethod dropdown-menu-item :link
  [owner idx itm]
  [:li.dropdown-menu-item
   {:key idx}
   [:a {:on-click (juxt (get itm :on-click (constantly nil))
                        #(toggle-dropdown! owner))}
    [:span (get itm :label)]]])

(defmethod dropdown-menu-item :divider
  [owner idx itm]
  [:li.divider {:role "separator", :key idx}])

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

(defn link-dropdown-toggle [owner]
  (let [{:keys [tab-index expanded toggle-icon-fn caret? toggle-label]}
        (om/get-state owner)]
    [:a.dropdown-toggle
     {:on-click       #(toggle-dropdown! owner)
      :data-toggle    "dropdown"
      :tab-index      tab-index
      :role           "button"
      :aria-haspopup  "true"
      :aria-expanded  expanded
      :on-mouse-enter #(start-hover! owner)
      :on-mouse-leave (fn [_]
                        (stop-hover!  owner)
                        (om/set-state! owner :expanded false))}
     (cond
      toggle-icon-fn
      [:span (toggle-icon-fn)
       (when caret?
         [:span.caret])]

      toggle-label
      [:span (str toggle-label " ")
       (when caret?
         [:span.caret])]
      )]))

;; TODO: implement.
(defn split-button-dropdown-toggle [owner]
  )

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
         [:li.dropdown
          {:class (concat [] (get state :classes))}
          (case (get state :toggle-type)
            :link
            (link-dropdown-toggle owner))
          
          (dropdown-menu owner)
          ])))))

