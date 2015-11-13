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
            [kanopi.view.widgets.input-field :as input-field]
            [kanopi.view.widgets.selector.dropdown :as dropdown]
            [kanopi.view.widgets.typeahead :as typeahead]

            [kanopi.util.browser :as browser]
            [kanopi.util.core :as util]
            ))

(defn prepare-fact [entity state]
  (let [{:keys [fact/attribute fact/value]} state]
    (assert (not-empty (get attribute :input-value))
            "Must have a value!")
    (assert (not-empty (get value     :input-value))
            "Must have a value!")
    (cond-> {}
      (get entity :db/id)
      (assoc :db/id (get entity :db/id))
      
      true
      (assoc :fact/attribute (hash-map (get attribute :input-type)
                                       (get attribute :input-value))
             :fact/value     (hash-map (get value :input-type)
                                       (get value :input-value)))
      )))

(def stroke-color "#dddddd")

(defn- handle-borders-group []
  [:g.handle-borders
   {:transform "translate(4, 4)"}
   [:path {:d "m 0 9 v -9 h 9"
           :stroke stroke-color
           :fill "transparent"}]
   [:path {:d "m 15 6 v 9 h -9"
           :stroke stroke-color
           :fill "transparent"}]])

(defn handle-fill
  [mode editing]
  (let [red    "red"
        yellow "yellow"
        green  "green"
        clear  "transparent"]
    (case mode
      :empty
      (if editing
        yellow
        red)

      :partial
      yellow

      :complete
      (if editing
        green
        clear))))

(defn any-value [entity]
  (or (get entity :input-value)
      (schema/get-value entity)))

(defn compute-mode [fact]
  (let [part-values (->> fact
                         ((juxt :fact/attribute :fact/value))
                         (map any-value))
        mode' (cond
               (every? not-empty part-values)
               :complete

               (some not-empty part-values)
               :partial

               (every? empty? part-values)
               :empty

               :default
               :error)]
    mode'))

(defn- handle-contents-group [& args]
  (let [{:keys [mode hovering editing] :or {mode :view, hovering false}}
        (apply hash-map args)

        fill (handle-fill mode editing)]
    [:g.handle-contents
     {:transform "translate(4, 4)"}
     [:g.handle-hover-contents
      {:transform "translate(2.5, 2.5)"}
      [:rect.handle-mode-indicator
       {:width "10"
        :height "10"
        :fill fill
        :opacity "0.4"}]]

     ]))


(defn available-input-types
  "TODO: use argument to filter and sort return value to give user the
  best possible list of available input types for the value entered."
  [input-value]
  (filter
   (fn [{:keys [ident label predicate] :as tp}]
     (cond
      (empty? input-value)
      true

      (predicate input-value)
      (predicate input-value)

      :default
      false))
   (vals schema/input-types)))

(defn entity->default-input-type
  ([ent]
   (entity->default-input-type ent nil))
  ([ent part]
   (let [ordering (get schema/input-types-ordered-by-fact-part-preference part [])
         default (->> ent
                      (schema/get-value)
                      (available-input-types)
                      (util/sort-by-ordering :ident ordering)
                      (first)
                      :ident)
         res (case (schema/describe-entity ent)
               :datum
               :datum/label

               :literal
               (schema/get-value-key ent default)

               :unknown
               default)
         ]
     res)))

(defn input-type->dropdown-menu-item
  [on-click-fn tp]
  (assoc tp :type :link, :on-click (partial on-click-fn (:value tp))))

(defn menu-item-comparator
  [input-value item1 item2]
  true)

(defn generate-menu-items [value on-click-fn ordering]
  (->> (available-input-types value)
       (util/sort-by-ordering :ident ordering)
       (mapv (partial input-type->dropdown-menu-item on-click-fn))
       ))

(defn handle [& args]
  (let [{:keys [mode hovering editing]} (apply hash-map args)]
    [:div.fact-handle
     [:svg {:width "24px" :height "24px"}
      (handle-borders-group)
      (handle-contents-group :mode mode :hovering hovering :editing editing)
      ]]))

(defn fact-part [owner part entity mode]
  (let [current-value (->> entity
                           ((juxt :input-value :datum/label :literal/text))
                           (some identity))
        ordered-input-types (get schema/input-types-ordered-by-fact-part-preference part [])
        ]
    [:div.fact-attribute
     [:div.view-fact-part.row
      [:div.inline-90-percent.col-xs-11
       (om/build typeahead/typeahead entity
                 {:react-key (str "view-fact-part" "-" part)
                  :state
                  {:element-type :input
                   :fact-part part}

                  :init-state
                  {:initial-input-value current-value
                   :input-value current-value
                   :display-fn schema/display-entity
                   :on-focus  (fn [v]
                                (om/set-state! owner :editing part)) 

                   :on-change (fn [v]
                                ;; TODO: update input type here.
                                (om/set-state! owner [part :input-value] v))
                   :on-submit (fn [v]
                                (let [state' (-> (om/get-state owner)
                                                 (assoc-in [part :input-value] v)
                                                 (assoc :editing nil))]
                                  (when (= :complete mode)
                                    (let [fact (prepare-fact entity state')
                                          datum-id (get state' :datum-id)
                                          msg  (if (:db/id fact)
                                                 (msg/update-fact datum-id fact)
                                                 (msg/add-fact    datum-id fact))]
                                      (msg/send! owner msg)))
                                  (om/set-state! owner state'))) 
                   ;                 :on-click  (partial handle-result-selection owner)
                   }
                  })
       ; NOTE: see edit-fact-part for example
       [:div.fact-part-metadata.container
        ;; TODO: this should not be a dropdown. it's a horizontal
        ;; sliding selector thing.
        (om/build dropdown/dropdown entity
                  {:init-state {:caret? true}
                   :state {:toggle-label (let [tp (get entity :input-type)
                                               _ (println "entity input-type" tp)]
                                           (cond (keyword? tp)
                                                 (name tp)
                                                 (map? tp)
                                                 (:label tp))) 
                           :menu-items (generate-menu-items
                                        current-value
                                        (fn [tp evt]
                                          (om/set-state! owner [part :input-type] tp))
                                        ordered-input-types
                                        )}})
        ]
       ]
      [:div.inline-10-percent.col-xs-1
       (when-let [id (get entity :db/id)]
         (->> (icons/open {})
              (icons/link-to owner [:datum :id id])))]]]  )
  )


(defn fact-next
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [default-attr-input-type (-> schema/input-types-ordered-by-fact-part-preference
                                        :fact/attribute
                                        (first)
                                        )
            default-value-input-type (-> schema/input-types-ordered-by-fact-part-preference
                                         :fact/value
                                         (first)
                                         )]
        {
         ; NOTE: this is not state, it's a derived value. It's in the
         ; function's name!!! Compute with each call to render-state.
         ; Modes #{:empty :partial :complete}
         ; :mode (compute-mode props)

         ; Editing states #{nil :attribute :value}
         ; Only registers as editing that state if the current value is
         ; nil. It's the marginal editing state. If you're editing an
         ; attribute where the fact already has an attribute, it does
         ; not affect the viability of the current state.
         :editing nil

         ; I need computed attribute and value values.
         ; Keep track of: input-value, input-matched-entity
         :fact/attribute (let [attr (get props :fact/attribute)]
                           (assoc attr
                                  :input-value (schema/get-value attr)
                                  ;; FIXME: default input-type must be
                                  ;; conditional on stuff.
                                  :input-type  (entity->default-input-type attr :fact/attribute))) 
         :fact/value     (let [valu (get props :fact/value)]
                           (assoc valu
                                  :input-value (schema/get-value valu)
                                  :input-type  (entity->default-input-type valu :fact/value)))

         }))

    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (let [curr-props (om/get-props owner)]
        (when (not= (:fact/attribute next-props) (:fact/attribute curr-props))
          (info "changed fact/attribute"))
        (when (not= (:fact/value next-props) (:fact/value curr-props))
          (info "changed fact/value"))))

    ; TODO: Synchronize props with component state when component updates

    om/IRenderState
    (render-state [_ {:keys [hovering editing fact/attribute fact/value] :as state}]
      (let [mode (compute-mode state)
            _ (println "Attribute:" attribute)
            _ (println "Value:" value)]
        (html
         [:div.fact-container.container
          [:div.fact-body.row
           {:on-mouse-enter #(om/set-state! owner :hovering true)
            :on-mouse-leave #(om/set-state! owner :hovering false)}
           [:div.inline-10-percent.col-xs-1
            (handle :mode mode :hovering hovering :editing editing)]
           [:div.inline-90-percent.col-xs-11
            (fact-part owner :fact/attribute attribute mode)
            (fact-part owner :fact/value value mode)
            ]]])))))

