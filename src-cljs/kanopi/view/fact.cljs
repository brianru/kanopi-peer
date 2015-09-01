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
            [kanopi.model.text :as text]
            [kanopi.util.browser :as browser]
            [kanopi.view.widgets.input-field :as input-field]
            [kanopi.view.widgets.dropdown :as dropdown]
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
    (render-state [_ {:keys [mode fact-hovering] :as state}]
      (let []
        (html
         [:div.fact-handle
          {:on-click #(msg/toggle-fact-mode! owner (get props :db/id))}
          [:svg {:width "24px"
                 :height "24px"}
           [:g.handle-borders
            {:transform "translate(4, 4)"}
            [:path {:d "m 0 9 v -9 h 9"
                    :stroke "black"
                    :fill "transparent"}]
            [:path {:d "m 15 6 v 9 h -9"
                    :stroke "black"
                    :fill "transparent"}]]
           [:g.handle-contents
            {:transform "translate(6.5, 6.5)"}
            (when fact-hovering
              [:g.handle-hover-contents
               [:rect.handle-mode-indicator
                {:width "10"
                 :height "10"
                 :fill "green"
                 :opacity "0.5"}]]
              )
            (when (= mode :edit)
              [:g.handle-edit-contents
               [:rect.handle-mode-indicator
                {:width "10"
                 :height "10"
                 :fill "green"}]
               [:rect.handle-cancel-edit]
               [:rect.handle-edit-history]
               [:rect.handle-confirm-edit]
               ]
              )
            ]
           ]

          ])))))

(defn fact-part
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-part-" (:db/id props)))

    om/IInitState
    (init-state [_]
      {:hovering false})

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let [part-type (schema/describe-entity props)]
        (html
         [:div.fact-attribute
          {:on-mouse-enter #(om/set-state! owner :hovering true)
           :on-mouse-leave #(om/set-state! owner :hovering false)}
          (cond

           (schema/thunk? props)
           (case mode
             :view
             [:div.view-fact-part
              [:a {:href (browser/route-for owner :thunk :id (:db/id props))}
               [:span.fact-part-representation
                (get props :thunk/label)]
               ]]

             :edit
             (let []
               [:div.edit-fact-part
                [:span.fact-part-representation
                 (get props :thunk/label)]
                [:div.fact-part-metadata-container
                 [:div.type-input
                  [:label.type-input-label "type:"]
                  [:span.type-input-value
                   (om/build dropdown/dropdown props
                             {:state {:toggle-label (name part-type)}
                              :init-state {:menu-items
                                           [{:type     :link
                                             :on-click println
                                             :label    "thunk"}
                                            {:type     :link
                                             :on-click println
                                             :label    "text"}]}})
                   ]]
                 [:div.value-input
                  [:label.value-input-label (str (text/entity-value-label props) ":")]
                  [:span.value-input-value
                   (om/build input-field/textarea props
                             {:init-state {:edit-key :thunk/label
                                           :new-value (get props :thunk/label)
                                           :submit-value #(do (println %))}})]]
                 ]]))

           (schema/literal? props)
           [:span (get props :value/string)]

           ;; TODO: spend some time on this case.
           (empty? props)
           [:div
            "Add a fact!"
            #_(om/build input-field/editable-value props
                        {:init-state {:edit-key :none
                                      :submit-value #(println %)
                                      :editing true
                                      :placeholder "Find or create an attribtue"}
                         :state (select-keys state [:mode :hovering])}
                        )]
           )
          ])))
    ))

(defn body
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-body-" (:db/id props)))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.fact-body.row
          {:on-mouse-enter #(om/set-state! owner :fact-hovering true)
           :on-mouse-leave #(om/set-state! owner :fact-hovering false)}
          [:div.inline-10-percent.col-xs-1
           (om/build handle props
                     {:state (select-keys state [:mode :fact-hovering])})]

          [:div.inline-90-percent.col-xs-11
           (om/build fact-part (first (get props :fact/attribute))
                     {:init-state {:fact (:db/id props)
                                   :fact-part :fact/attribute}
                      :state (select-keys state [:mode :fact-hovering])})

           (om/build fact-part (first (get props :fact/value))
                     {:init-state {:fact (:db/id props)
                                   :fact-part :fact/value}
                      :state (select-keys state [:mode :fact-hovering])})]
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
