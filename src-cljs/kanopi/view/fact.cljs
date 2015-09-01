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
            [kanopi.view.widgets.input-field :as input-field]
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

(defn attribute
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-attribute-" (:db/id props)))

    om/IInitState
    (init-state [_]
      {:hovering false})

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let [_ (println props)]
        (html
         [:div.fact-attribute
          {:on-mouse-enter #(om/set-state! owner :hovering true)
           :on-mouse-leave #(om/set-state! owner :hovering false)}
          (cond

           (schema/thunk? props)
           [:div
            (om/build input-field/editable-value props
                      {:init-state {:edit-key :thunk/label
                                    :submit-value #(println %)}
                       :state (select-keys state [:mode :hovering])})
            (->> (icons/open {})
                 (icons/link-to owner [:thunk :id (:db/id props)])) ]

           (schema/literal? props)
           [:span (get props :value/string)]

           (empty? props)
           [:div
            (om/build input-field/editable-value props
                      {:init-state {:edit-key :none
                                    :submit-value #(println %)
                                    :editing true
                                    :placeholder "Find or create an attribtue"}
                       :state (select-keys state [:mode :hovering])}
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
