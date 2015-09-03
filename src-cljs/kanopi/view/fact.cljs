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
            [kanopi.view.widgets.typeahead :as typeahead]
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
          {:on-click #(->> (msg/toggle-fact-mode props)
                           (msg/send! owner))}
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
  "Here's how this works.

  Fact parts are selection / initialziation mechanisms.

  The goal is to connect the parent entity to another entity via an attribute.

  We are not modifying any entity besides the parent (really, the
  fact, which is referenced by the parent).

  I need a typeahead that renders entities with similar values or
  labels. This is tricky because labels can be long, values can be
  images or latex.

  First the user determines the type of the fact part. This allows the
  typeahead on the value to be useful. This is a dropdown menu. It
  configures the typeahead search that follows.

  Then the user begins populating the value.

  "
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-part-" (:db/id props)))

    om/IInitState
    (init-state [_]
      {:hovering false
       
       :selected-type nil
       :entered-value ""
       :matching-entity props
       })

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let [part-type (schema/describe-entity props)]
        (html
         [:div.fact-attribute
          {:on-mouse-enter #(om/set-state! owner :hovering true)
           :on-mouse-leave #(om/set-state! owner :hovering false)}
          (case mode
            :view
            [:div.view-fact-part
             [:a {:href (when-let [id (:db/id props)]
                          (browser/route-for owner :thunk :id id))}
              [:span.fact-part-representation
               (schema/display-entity props)]
              ]]

            :edit
            (let [{:keys [selected-type entered-value matching-entity]}
                  state]
              [:div.edit-fact-part
               [:span.fact-part-representation
                (schema/display-entity props)]
               [:div.fact-part-metadata-container
                [:div.type-input
                 [:label.type-input-label "type:"]
                 [:span.type-input-value
                  (om/build dropdown/dropdown props
                            {:state
                             {:toggle-label (name (or selected-type part-type))}

                             :init-state
                             {:menu-items
                              [{:type     :link
                                :value    :thunk
                                :label    "thunk"
                                :on-click (fn [_]
                                            (om/set-state! owner :selected-type :thunk))
                                }
                               {:type     :link
                                :value    :literal/text
                                :label    "text"
                                :on-click (fn [_]
                                            (om/set-state! owner :selected-type :literal/text))
                                }]}})]]
                [:div.value-input
                 [:label.value-input-label (str (text/entity-value-label props) ":")]
                 [:span.value-input-value
                  #_(om/build input-field/textarea props
                              {:init-state
                               {:edit-key     :thunk/label
                                :new-value    (get props :thunk/label)
                                :submit-value (fn [v]
                                                (->> (msg/update-entity-value props v)
                                                     (msg/send! owner)))}})
                  (om/build typeahead/typeahead props
                            {:init-state {:element-type :textarea
                                          }})]]
                ]]))
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
                              :fact/attribute #{{:db/id nil}}
                              :fact/value     #{{:db/id nil}}})
         )))))
