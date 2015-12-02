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
  vs a piece of paper.
  
  TODO: keyboard shortcuts for navigating between facts and fact-parts.
  GOAL: rely on tabindex as much as possible."
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]
            [kanopi.view.icons :as icons]
            [kanopi.aether.core :as aether]
            [kanopi.view.widgets.selector.dropdown :as dropdown]
            [kanopi.view.widgets.selector.pills :as pills]
            [kanopi.view.widgets.typeahead :as typeahead]

            [kanopi.util.browser :as browser]
            [kanopi.util.core :as util]
            ))

(defn fact-part-link [owner fact-part]
  (when-let [id (get fact-part :db/id)]
    (case (schema/describe-entity fact-part)
      :datum
      (->> (icons/open {})
           (icons/link-to owner [:datum :id id]))
      
      :literal
      (->> (icons/edit {})
           (icons/link-to owner [:literal :id id]))
      
      ; default
      nil)))

(defn prepare-fact [fact-state]
  (let [{:keys [fact/attribute fact/value]} fact-state]
    (assert (get attribute :parsed-value)
            "Must have a value!")
    (assert (get value     :parsed-value)
            "Must have a value!")
    (cond-> {}
      (get fact-state :db/id)
      (assoc :db/id (get fact-state :db/id))
      
      true
      (assoc :fact/attribute (hash-map (get-in attribute [:input-type :ident])
                                       (get attribute :parsed-value))
             :fact/value     (hash-map (get-in value [:input-type :ident])
                                       (get value :parsed-value)))
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
                         (map :input-value))
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


(defn entity->default-input-type
  ([ent]
   (entity->default-input-type ent nil))
  ([ent part]
   (let [ordering (get schema/input-types-ordered-by-fact-part-preference part [])
         default (->> ent
                      (schema/get-value)
                      (schema/compatible-input-types)
                      (util/sort-by-ordering :ident ordering)
                      (first))
         ]
     (or (schema/get-input-type ent) default))))

(defn input-type->dropdown-menu-item
  [on-click-fn tp]
  (assoc tp :type :link, :on-click (partial on-click-fn (:ident tp))))

(defn menu-item-comparator
  [input-value item1 item2]
  true)

(defn generate-menu-items [value on-click-fn ordering]
  (->> (schema/compatible-input-types value)
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

(defn update-fact-part
  "Fact parts driven by 3 pieces of data:
  - input-value
  - input-type
  - parsed-value
  "
  ([fact part]
   (update-fact-part fact part
                     ;; call to `str` because input-values are always
                     ;; strings! later gets parsed based on the
                     ;; input-type
                     (or (get fact :input-value) (str (schema/get-value fact)))
                     (or (get fact :input-type)  (schema/get-input-type fact))
                     ))

  ([fact part input-value]
   (update-fact-part fact part input-value
                     (or (get fact :input-type)  (schema/get-input-type fact))))

  ([fact part input-value input-type]
   (let [type-predicate (get input-type :predicate (constantly nil))
         input-type     (if (type-predicate input-value)
                          input-type
                          (entity->default-input-type
                            (assoc fact :input-value input-value) part))
         parsed-value   ((:parser input-type) input-value)]
     (assoc fact
            :input-value  input-value
            :input-type   input-type
            :parsed-value parsed-value))))

 (defn reset-fact-state [props state]
   (assoc state
          :editing nil
          :submitting nil
          :fact/attribute (update-fact-part nil :fact/attribute)
          :fact/value (update-fact-part nil :fact/value)
          ))

;; FIXME: this is very hard to follow.
(defn handle-submission!
  [owner part-key fact-part & args]
  (let [state' (apply assoc (om/get-state owner) part-key fact-part :submitting true
                      args)
        mode   (compute-mode state')]
    (if-not (= :complete mode)
      (om/set-state! owner state')
      (let [fact     (prepare-fact state')
            datum-id (get state' :datum-id)
            msg      (if (:db/id fact)
                       (msg/update-fact datum-id fact)
                       (msg/add-fact    datum-id fact))]
        (msg/send! owner msg)
        (if (:db/id fact)
          (do
           (println "UPDATE FACT")
           (->> (msg/update-fact datum-id fact)
                (msg/send! owner))
           (om/set-state! owner state'))
          (do
           (println "ADD FACT")
           (->> (msg/add-fact datum-id fact)
                (msg/send! owner))
           (om/set-state! owner (reset-fact-state (om/get-props owner) state'))))))))

(defn fact-part [owner part-key fact-part mode]
  (let [ordered-input-types
        (get schema/input-types-ordered-by-fact-part-preference part-key [])
        ]
    [:div.fact-attribute
     [:div.view-fact-part.row
      [:div.inline-90-percent.col-xs-11
       (om/build typeahead/typeahead fact-part
                 (merge {:react-key (str "view-fact-part" "-" part-key)}
                        (typeahead/editor-config
                         :element-type      :input
                         :input-value       (get fact-part :input-value)
                         :input-placeholder (name part-key)
                         :input-on-change
                         (fn [input-value]
                           (om/update-state! owner part-key
                                             #(update-fact-part % part-key input-value)))
                         :input-on-blur
                         (fn [v]
                           (om/update-state! owner :editing
                                             (fn [existing]
                                               (if (= part-key existing)
                                                 nil existing))))
                         :input-on-focus
                         (fn [v] (om/set-state! owner :editing part-key))

                         :result-display-fn
                         schema/display-entity
                         :result-on-click
                         (fn [result]
                                (println "HANDLE THIS!!!" result)
                                (println "MUST extend fact-part to take datums as inputs"))
                         :on-submit
                         (fn [v]
                           (handle-submission! owner part-key
                                               (update-fact-part
                                                (om/get-state owner part-key)
                                                part-key v)
                                               :editing nil))
                         )))
       [:div.fact-part-metadata
        (om/build pills/horizontal fact-part
                  {:state {:current-item
                           (om/get-state owner [part-key :input-type :ident])

                           :menu-items
                           (generate-menu-items
                            (get-in fact-part [:input-value]) 
                            (fn [tp evt]
                              (let [input-type' (get schema/input-types tp)
                                    fact-part   (om/get-state owner part-key)
                                    fact-part'  (update-fact-part fact-part part-key
                                                                  (get fact-part :input-value)
                                                                  input-type')]
                                (handle-submission! owner part-key fact-part')))
                            ordered-input-types)}})
        ]
       ]
      [:div.inline-10-percent.col-xs-1
       (fact-part-link owner fact-part)
       #_(when-let [id (get fact-part :db/id)]
           (->> (icons/open {})
                (icons/link-to owner [:datum :id id])))]]]))


(defn fact-next
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let []
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
         :fact/attribute (update-fact-part (get props :fact/attribute) :fact/attribute) 
         :fact/value     (update-fact-part (get props :fact/value)     :fact/value)

         }))

    ; TODO: Synchronize props with component state when component updates

    om/IRenderState
    (render-state [_ {:keys [hovering editing fact/attribute fact/value] :as state}]
      (let [mode (compute-mode state)
            ]
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

