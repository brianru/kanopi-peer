(ns kanopi.view.fact
  "Facts feel like carefully assembled little devices. They are
  precise yet frictionless. Confirmation is not required, though
  everything can be undone. Hovering over a fact reveals all available
  functionality. Nothing is hidden behind a click. The state of a fact
  is visible with a single glance at a single indicator."
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.model.schema :as schema]
            [kanopi.view.icons :as icons]
            [kanopi.ether.core :as ether]
            [kanopi.model.message :as msg]
            ))

(defn handle
  "TODO: anchor for drag & drop reordering of facts
  TODO: "
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-handle-" (:db/id props)))

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let [_ (println mode)]
        (html
         [:div.fact-handle
          {:on-click #(msg/toggle-fact-mode! owner (get props :db/id))}
          [:div.fact-handle-top
           [:div.fact-handle-top-left]
           [:div.fact-handle-top-right]]
          [:div.fact-handle-bottom
           [:div.fact-handle-bottom-left]
           [:div.fact-handle-bottom-right]]
          [:div.fact-handle-icon
           {:style {:display (case mode
                               :view "none"
                               :edit "inherit")}
            }]
          ])))))

;;(defmulti fact-part
;;  (fn [attr part]
;;    [attr (schema/describe-entity part)]))
;;
;;(defmethod fact-part [:fact/attribute :thunk])
;;(defmethod fact-part [:fact/attribute :literal])
;;(defmethod fact-part [:fact/value :thunk])
;;(defmethod fact-part [:fact/value :literal])

(defn- start-edit [e owner korks]
  (om/set-state! owner korks true))

(defn- handle-change [e owner korks]
  (om/set-state! owner korks (.. e -target -value)))

(defn- end-edit [e owner korks handler-fn]
  (om/set-state! owner korks false)
  (handler-fn (.. e -target -value))
  (om/set-state! owner :new-value nil))

(defn editable-value [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      ;; focus on text field when editing an input field
      (when (and (not (get prev-state :editing))
                 (om/get-state owner :editing))
       (. (om/get-node owner "text-field") (focus))))

    om/IRenderState
    (render-state [_ {:keys [editing edit-key submit-value]
                      :as state}]
      (let []
        (html
         [:span
          [:span
             {:style {:display (when editing "none")}
              :on-click #(start-edit % owner :editing)}
             (get props edit-key)]
          [:input
           {:style {:display (when-not editing "none")}
            :ref "text-field"
            :type "text"
            :value (get state :new-value)
            :placeholder (get state :placeholder)
            :on-change #(handle-change % owner :new-value)
            :on-key-down #(when (= (.-key %) "Enter")
                            (end-edit % owner :editing submit-value))
            :on-blur #(end-edit % owner :editing submit-value)}]

          ])))))

(defn attribute
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-attribute-" (:db/id props)))

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let [_ (println props)]
        (html
         [:div.fact-attribute
          (cond

           (schema/thunk? props)
           [:div
            (om/build editable-value props
                      {:init-state {:edit-key :thunk/label
                                    :submit-value #(println %)}})
            (->> (icons/open {})
                 (icons/link-to owner [:thunk :id (:db/id props)])) ]

           (schema/literal? props)
           [:span (get props :value/string)]

           (empty? props)
           [:div
            (om/build editable-value props
                      {:init-state {:edit-key :none
                                    :submit-value #(println %)
                                    :editing true
                                    :placeholder "Find or create an attribtue"}
                       :state (select-keys state [:mode])}
                      )]
           )
          ])))
    ))

(defn value
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-value-" (:id props)))

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let []
        (html
         [:div.fact-value
          (cond
           (schema/thunk? props)
           [:span (get props :thunk/label)]

           (schema/literal? props)
           [:span (get props :value/string)])
          ])))
    ))

(defn body
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-body-" (:id props)))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.fact-body.row
          [:div.inline-10-percent.col-md-1
           (om/build handle props {:state (select-keys state [:mode])})]

          [:div.inline-90-percent.col-md-11
           (om/build attribute (first (get props :fact/attribute))
                     {:state (select-keys state [:mode])})
           (om/build value     (first (get props :fact/value))
                     {:state (select-keys state [:mode])})]
          ])))
    ))

(defn container
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact" (:db/id props)))

    om/IInitState
    (init-state [_]
      {:mode :view})

    om/IWillMount
    (will-mount [_]
      (ether/listen! owner :noun [:fact (:db/id props)]
                     (fn [{:keys [noun verb context]}]
                       (case verb
                         :toggle-mode
                         (let [new-mode 
                               (if (= :view (om/get-state owner :mode))
                                 :edit
                                 :view)]
                           (om/set-state! owner :mode new-mode))
                         ;;default
                         (debug "what?" noun verb)
                         ))
                     #_(partial println "here")))
    om/IWillUnmount
    (will-unmount [_]
      (ether/stop-listening! owner))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.fact-container.container
          (om/build body props {:state (select-keys state [:mode])})])))
    ))

(defn new-fact
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "add-fact"))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         (om/build container {:db/id nil
                              :fact/attribute #{}
                              :fact/value     #{}})
         )))))
