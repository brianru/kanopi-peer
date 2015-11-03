(ns kanopi.view.fact
  "Facts feel like carefully assembled little devices. They are
  precise yet frictionless. Confirmation is not required, though
  everything can be undone. Hovering over a fact reveals all available
  functionality. Nothing is hidden behind a click. The state of a fact
  is visible with a single glance at a single indicator.
  
  I am not sure how accurate the above statement should be. I want
  quick actions be to be but a single click away, though I do want to
  hide the more involved edits from the user when the user is reading.
  Reading, writing (quick edits) and full-on constructing are
  different things. Think outlines or storyboarding vs a text editor
  vs a piece of paper."
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]
            [kanopi.model.text :as text]
            [kanopi.view.icons :as icons]
            [kanopi.aether.core :as aether]
            [kanopi.util.browser :as browser]
            [kanopi.view.widgets.input-field :as input-field]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.view.widgets.typeahead :as typeahead]
            ))

(def stroke-color "#dddddd")

(defn- handle-borders-group [mode]
  [:g.handle-borders
   {:transform "translate(4, 4)"}
   [:path {:d "m 0 9 v -9 h 9"
           :stroke stroke-color
           :fill "transparent"}]
   [:path {:d "m 15 6 v 9 h -9"
           :stroke stroke-color
           :fill "transparent"}]])

(defn- handle-contents-group [& args]
  (let [{:keys [mode hovering] :or {mode :view, hovering false}}
        (apply hash-map args)]
    [:g.handle-contents
     {:transform "translate(4, 4)"}
     (when (or (= mode :edit) hovering)
       [:g.handle-hover-contents
        {:transform "translate(2.5, 2.5)"}
        [:rect.handle-mode-indicator
         {:width "10"
          :height "10"
          :fill "green"
          :opacity "0.4"}]]
       )]))

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

;; FIXME: this should not be a separate component. make it an html
;; template and SIMPLIFY.
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
          [:svg {:width "24px", :height "24px"}
           (handle-borders-group mode)
           (handle-contents-group
            :mode mode
            :hovering fact-hovering
            :submit-handler (fn []
                              (->> (msg/update-fact (om/get-state owner :datum-id)
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

(defn render-fact-part [m]
  [:span.fact-part-representation
   (schema/display-entity m)])

(defn view-fact-part
  "One click away from editing in place and navigating to value.
  "
  [owner m k]
  [:div.view-fact-part.row
   [:div.inline-90-percent.col-xs-11
    (om/build typeahead/typeahead m
              {:react-key (str "view-fact-part" "-" k)
               :state
               {:element-type :input
                :fact-part k}

               :init-state
               {:input-value (schema/display-entity m)
                :on-click  (partial handle-result-selection owner)
                :on-submit (partial handle-input-submission owner)}
               })]
   [:div.inline-10-percent.col-xs-1
    (when-let [id (get m :db/id)]
      (->> (icons/open {})
           (icons/link-to owner [:datum :id id])))]])

(defn available-input-types
  "TODO: use argument to filter and sort return value to give user the
  best possible list of available input types for the value entered."
  [value]
  (vector
   {:value :datum
    :label "datum"}

   {:value :literal/text
    :label "text"}))

(defn input-type->dropdown-menu-item
  [on-click-fn tp]
  (assoc tp :type :link, :on-click (partial on-click-fn (:value tp))))

(defn menu-item-comparator
  [input-value item1 item2]
  true)

(defn generate-menu-items [value on-click-fn]
  (->> (available-input-types value)
       (mapv (partial input-type->dropdown-menu-item on-click-fn))
       (sort-by :value (partial menu-item-comparator value))))

(defn edit-fact-part
  ""
  [owner m k]
  (let [{:keys [selected-type part-type entered-value matching-entity]}
        (om/get-state owner)]
    [:div.edit-fact-part
     (om/build typeahead/typeahead m
                {:react-key (str "edit-fact-part" "-" k)
                 :state
                 {:element-type :input
                  :fact-part k}
                 :init-state
                 {:input-value (schema/display-entity m)
                  :on-click    (partial handle-result-selection owner)
                  :on-submit   (partial handle-input-submission owner)}})
     [:div.fact-part-metadata-container
      (om/build dropdown/dropdown m
                {:react-key (str "edit-fact-part" "-" k)
                 :state
                 {:toggle-label (name (or selected-type part-type))
                  :menu-items   (generate-menu-items
                                 entered-value (partial handle-type-selection owner))}})
      ]])
  )

(defn fact-part
  "Here's how this works.

  Fact parts are selection / initialziation mechanisms.

  The goal is to connect the parent entity to another entity via an attribute.

  We are not modifying any entity besides the parent (really, the
  fact, which is referenced by the parent).

  So here are the states:
  View is actually editable, it's a typeahead input field.
  Edit is a big textarea with buttons to do fancy things.

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
    (render-state [_ {:keys [mode fact-part] :as state}]
      (let [part-type (schema/describe-entity props)]
        (html
         [:div.fact-attribute
          {:on-mouse-enter #(om/set-state! owner :hovering true)
           :on-mouse-leave #(om/set-state! owner :hovering false)}

          (case mode
            :view
            (view-fact-part owner props fact-part)

            :edit
            (view-fact-part owner props fact-part)
            #_(edit-fact-part owner props fact-part)
            #_(let [{:keys [selected-type entered-value matching-entity]}
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
                               {:toggle-label (name (or selected-type part-type))
                                :menu-items (generate-menu-items
                                             entered-value (partial handle-type-selection owner))
                                }
                               })]]
                  [:div.value-input
                   [:label.value-input-label (str (text/entity-value-label props) ":")]
                   [:span.value-input-value
                    (om/build typeahead/typeahead props
                              {:state
                               {:element-type (browser/input-element-for-entity-type
                                               (or selected-type part-type))
                                :fact-part    fact-part}

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
                              state [:mode :datum-id :fact-hovering
                                     :fact/attribute :fact/value])})]

          [:div.inline-90-percent.col-xs-11
           ;; BEWARE: (get props :fact/attribute) returns a set which
           ;; is not compatible with Om cursors. Maps and vectors
           ;; only.)
           ;; NOTE: is this still true? I changed the schema and am
           ;; now using pull syntax for queries, attributes and values
           ;; should not be in collections
           (om/build fact-part (get props :fact/attribute)
                     {:init-state {:fact (:db/id props)
                                   :fact-part :fact/attribute}
                      :state (select-keys state [:mode :fact-hovering])})

           (om/build fact-part (get props :fact/value)
                     {:init-state {:fact (:db/id props)
                                   :fact-part :fact/value}
                      :state (select-keys state [:mode :fact-hovering])})
           
           (when (= :edit (get state :mode))
             [:div.fact-edit-controls.row
              
              ])]
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
      (aether/listen! owner :noun [:fact (:db/id props)]
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
                            (om/update-state!
                             owner (get context :fact-part)
                             (partial merge {:input-value (get context :value)})))

                          ;;default
                          (debug "what?" noun verb)
                          ))
                      #_(partial println "here")))
    om/IWillUnmount
    (will-unmount [_]
      (aether/stop-listening! owner))

    om/IWillUpdate
    (will-update [this next-props next-state]
      #_(info "will-update" next-state))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.fact-container.container
          ;; TODO: receiving (get state :fact-count). if (= 1 %) and
          ;; (nil? (:db/id %)) then initialize in edit mode.
          (om/build body props
                    {:state (select-keys state [:mode :datum-id :fact/attribute :fact/value])}
                    )] )))
    ))

(defn new-fact-template
  "The mechanics of initializing a fact are drastically different from
  those of viewing and editing an existing fact. Therefore, this
  component will re-use many of the html templates for the normal
  container and fact-body components while offering a tuned experience
  for initializing facts.
  "
  [props owner opts]
  (reify
    om/IRenderState
    (render-state [_ {:keys [hovering] :as state}]
      (let []
        (html
         [:div.fact-container.container
          {:on-mouse-enter #(om/set-state! owner :hovering true)
           :on-mouse-leave #(om/set-state! owner :hovering false)}
          [:div.fact-body.row

           [:div.inline-10-percent.col-xs-1
            ;; TODO: i'm repeating some code!
            [:div.fact-handle
             {:on-click (constantly nil)}
             [:svg {:width "24px", :height "24px"}
              (handle-borders-group :view)
              (handle-contents-group
               :mode :view
               :hovering hovering
               :cancel-handler  (constantly nil)
               :history-handler (constantly nil)
               :submit-handler  (constantly nil))]]
            ]

           [:div.inline-90-percent.col-xs-11
            [:div.fact-attribute
             (view-fact-part owner {:literal/text "Click here"} :fact/attribute)]
            [:div.fact-attribute
             (view-fact-part owner {:literal/text "to add a fact"} :fact/value)]]]
          ])))))
