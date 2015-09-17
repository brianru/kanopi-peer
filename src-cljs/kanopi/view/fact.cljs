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
            [kanopi.model.message :as msg]
            [kanopi.model.text :as text]
            [kanopi.view.icons :as icons]
            [kanopi.ether.core :as ether]
            [kanopi.util.browser :as browser]
            [kanopi.view.widgets.input-field :as input-field]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.view.widgets.typeahead :as typeahead]
            ))

(defn- handle-borders-group [mode]
  [:g.handle-borders
   {:transform "translate(4, 4)"}
   [:path {:d "m 0 9 v -9 h 9"
           :stroke "black"
           :fill "transparent"}]
   [:path {:d "m 15 6 v 9 h -9"
           :stroke "black"
           :transform (when (= mode :edit)
                        "translate(0, 48)")

           :fill "transparent"}]])

(defn- handle-contents-group [& args]
  (let [{:keys [mode hovering cancel-handler history-handler submit-handler]
         :or {mode :view
              hovering false
              cancel-handler  (constantly nil)
              history-handler (constantly nil)
              submit-handler  (constantly nil)}}
        (apply hash-map args)]
    [:g.handle-contents
     {:transform "translate(4, 4)"}
     (cond

      (and (= mode :view) hovering)
      [:g.handle-hover-contents
       {:transform "translate(2.5, 2.5)"}
       [:rect.handle-mode-indicator
        {:width "10"
         :height "10"
         :fill "green"
         :opacity "0.5"}]]

      (= mode :edit)
      [:g.handle-edit-contents
       [:g {:transform "translate(0,0)"}
        (icons/svg-clear
         {:transform (icons/transform-scale :from 48 :to 16)
          :fill "black"})
        [:rect.click-area
         {:width 16
          :height 16
          :fill "green"
          :on-click cancel-handler
          }]]
       [:g {:transform "translate(0,24)"}
        (icons/svg-restore
         {:transform (icons/transform-scale :from 48 :to 16)
          :fill "lightgray"})
        [:rect.click-area
         {:width 16
          :height 16
          :fill "green"
          :style {:cursor "inherit"}
          :on-click history-handler
          }]]
       [:g {:transform "translate(0,48)"}
        (icons/svg-done
         {:transform (icons/transform-scale :from 48 :to 16)
          :class "handle-icon"
          :fill "black"})
        [:rect.click-area
         {:width 16
          :height 16
          :fill "green"
          :on-click submit-handler
          }]]

       ])]))

(defn- merge-updated-data [owner]
  (let [attr'  (om/get-state owner :fact/attribute)
        value' (om/get-state owner :fact/value)
        fact   (om/get-props owner)]
    {:db/id (:db/id fact)
     :fact/attribute [(->> attr'
                           (merge (first (:fact/attribute fact)))
                           ((juxt :db/id identity))
                           (some identity))]
     :fact/value     [(->> value'
                           (merge (first (:fact/value fact)))
                           ((juxt :db/id identity))
                           (some identity))]
     }))

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
         ;; TODO: animate all the width, height and transform
         ;; attributes
         [:div.fact-handle
          {:on-click #(->> (msg/toggle-fact-mode props)
                           (msg/send! owner))}
          [:svg {:width "24px"
                 :height (if (= mode :view) "24px" "72px")}
           (handle-borders-group mode)
           (handle-contents-group
            :mode mode
            :hovering fact-hovering
            :cancel-handler (constantly nil)
            :history-handler (constantly nil)
            :submit-handler (fn []
                              (->> (msg/update-fact (om/get-state owner :thunk-id)
                                                    (merge-updated-data owner))
                                   (msg/send! owner))))
           ]])))))

(defn- handle-type-selection [owner tp evt]
  (om/set-state! owner :selected-type tp)
  (->> (msg/select-fact-part-type (om/get-state owner :fact)
                                  (om/get-state owner :fact-part)
                                  tp)
       (msg/send! owner)))

(defn- handle-result-selection [owner res evt]
  (om/update-state!
   owner
   (fn [existing]
     (assoc existing
            :selected-type (schema/describe-entity res)
            :entered-value (schema/get-value res)
            :matching-entity res)))
  (->> (msg/select-fact-part-reference (om/get-state owner :fact)
                                       (om/get-state owner :fact-part)
                                       res)
       (msg/send! owner)))

(defn- handle-input-submission
  "This is always used to create new entities.
  FIXME: this fails when there was previously a matching entity."
  [owner value evt]
  (let [res (schema/create-entity (om/get-state owner :selected-type) value)]
    (om/update-state!
     owner
     (fn [existing]
       (assoc existing
              :selected-type (schema/describe-entity res)
              :entered-value (schema/get-value res)
              :matching-entity res)))
    (->> (msg/select-fact-part-reference (om/get-state owner :fact)
                                         (om/get-state owner :fact-part)
                                         res)
         (msg/send! owner))))

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

       :selected-type (let [desc (schema/describe-entity props)]
                        (if (= :unknown desc)
                          :literal/text
                          desc))
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

          (when true ;;; dev-mode
            [:span (str (:fact-part state)
                        ": " (get-in state [:matching-entity :db/id]))])

          (case mode
            :view
            [:div.view-fact-part
             [:a {:href (when-let [id (:db/id props)]
                          (browser/route-for owner :thunk :id id))}
              [:span.fact-part-representation
               (schema/display-entity props)]]]

            :edit
            (let [{:keys [selected-type entered-value matching-entity]}
                  state
                  ]
              [:div.edit-fact-part
               [:span.fact-part-representation
                (schema/display-entity matching-entity)]

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
                                :on-click (partial handle-type-selection owner :thunk)
                                }
                               {:type     :link
                                :value    :literal/text
                                :label    "text"
                                :on-click (partial handle-type-selection owner :literal/text)
                                }]}})]]
                [:div.value-input
                 [:label.value-input-label (str (text/entity-value-label props) ":")]
                 [:span.value-input-value
                  (om/build typeahead/typeahead props
                            {
                             :state
                             {
                              :element-type (browser/input-element-for-entity-type
                                             (or selected-type part-type))
                              :fact-part (get state :fact-part)
                              }
                             :init-state
                             {:input-value (schema/display-entity props)
                              :on-click    (partial handle-result-selection owner)
                              :on-submit   (partial handle-input-submission owner)
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
                     {:state (select-keys
                              state [:mode :thunk-id :fact-hovering
                                     :fact/attribute :fact/value])})]

          [:div.inline-90-percent.col-xs-11
           ;; BEWARE: (get props :fact/attribute) returns a set which
           ;; is not compatible with Om cursors. Maps and vectors
           ;; only.)
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
  {:pre [(om/cursor? props)]}
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact" (:db/id props)))

    om/IInitState
    (init-state [_]
      {:mode :view
       :fact/attribute {:db/id nil}
       :fact/value     {:db/id nil}})

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

                         :select-fact-part-type
                         (om/update-state!
                          owner (get context :fact-part)
                          #(merge % {:selected-type (get context :value)}))

                         :select-fact-part-reference
                         (let []
                           (om/update-state!
                            owner (get context :fact-part)
                            #(merge % (get context :selection))))

                         :input-fact-part-value
                         (let []
                           (println "here!")
                           (om/update-state!
                            owner (get context :fact-part)
                            (partial merge {:input-value (get context :value)})))

                         ;;default
                         (debug "what?" noun verb)
                         ))
                     #_(partial println "here")))
    om/IWillUnmount
    (will-unmount [_]
      (ether/stop-listening! owner))

    om/IWillUpdate
    (will-update [this next-props next-state]
      #_(info "will-update" next-state))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.fact-container.container
          (om/build body props
                    {:state (select-keys state [:mode :thunk-id :fact/attribute :fact/value])}
                    )])))
    ))
