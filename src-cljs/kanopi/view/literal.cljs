(ns kanopi.view.literal
  "TODO for literal view/edit support:
  ;; 1 - stub component
  ;; 2 - routing
  ;; 3 - modify all links to check and use :datum vs :literal as appropriate
  ;; 4 - where do I want to store current-literal data in app-state? 
  5 - header, intent, breadcrumbs
  6 - 

  What does the user need to know to edit a literal?
  - datum + fact[attribute] => fact[literal]
  - other compatible types
  - all other types, if the user wants.
  - history of changes (AND contributors)

  Related verbs:
  x :literal/get
  x :literal/update

  "
  (:require [om.core :as om]
            [schema.core :as s]
            [sablono.core :refer-macros (html) :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]

            [kanopi.view.widgets.text-editor :as text-editor]
            [kanopi.view.widgets.input-field :as input-field]
            [kanopi.view.widgets.math :as math]

            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]

            [kanopi.util.browser :as browser]
            [kanopi.util.core :as util]
            ))

(defmulti literal-renderer
  "Some renderers take advantage of persisted as part of the literal
  entity, others use only the raw value."
  (fn [_ literal]
    (some-> (schema/get-input-type literal)
            (get :ident)
            (name)
            (keyword))))

(defmethod literal-renderer :math
  [owner literal]
  (om/build math/katex literal
            {:state {:input-value (or (om/get-state owner [:input-value])
                                      (get literal :literal/math))}}))

(defmulti literal-editor
  "
  Strategy:
  1 - CodeMirror for everything
  2 - Add ProseMirror for text, use CodeMirror for everything else
  3 - Add katex rendering to a Math mode using CodeMirror set to Latex
  4 - 

  Meanwhile:
  - design other literal UIs
  - no literals are immutable, but must communicate to user the
  idea that literals are shared with all the datums in that left
  sidebar

  "
  (fn [_ literal]
    (some-> (schema/get-input-type literal)
            (get :ident)
            (name)
            (keyword)))
   :default :text
  )

(defmethod literal-editor :math
  [owner literal]
  (om/build text-editor/code literal
            (text-editor/code-editor-config
              :edit-key   :literal/math
              :input-type :literal/math
              :input-value (get literal :literal/math)
              :on-change  (fn [value]
                            (om/set-state! owner :input-value value))
              :on-submit  (fn [value]
                            (->> (msg/update-literal (:db/id literal) :literal/math value)
                                 (msg/send! owner))))
            ))

(defmethod literal-editor :text
  [owner literal]
  (om/build text-editor/rich-text literal
            {:init-state
             {:edit-key :literal/text
              :input-type :literal/text
              :input-value (get literal :literal/text)
              :on-change (fn [value]
                           (om/set-state! owner :input-value value))
              :on-submit (fn [value]
                           (->> (msg/update-literal (:db/id literal) :literal/text value)
                                (msg/send! owner)))}}))

(defmethod literal-editor :integer
  [owner literal]
  (om/build input-field/integer literal
            {:init-state
             {:initial-input-value (get literal :literal/integer)
              :on-change (fn [value]
                           (om/set-state! owner :input-value value))
              :on-submit (fn [value]
                           (->> (msg/update-literal (:db/id literal) :literal/integer value)
                                (msg/send! owner)))}}))

(defmethod literal-editor :decimal
  [owner literal]
  (om/build input-field/decimal literal
            {:init-state
             {:initial-input-value (get literal :literal/decimal)
              :on-change (fn [value]
                           (om/set-state! owner :input-value value))
              :on-submit (fn [value]
                           (->> (msg/update-literal (:db/id literal) :literal/decimal value)
                                (msg/send! owner)))}}))

(defmethod literal-editor :uri
  [owner literal]
  (om/build input-field/editable-value literal
            {:init-state
             {:edit-key     :literal/uri
              :submit-value (fn [value]
                              (->> (msg/update-literal (:db/id literal) :literal/uri value)
                                   (msg/send! owner)))
              }}))

(defn literal-context
  "A vertical list of somewhat stylized relating entities. Each is a
  link to that entity and maybe hovering renders a popover with more
  information. The font size is relatively small."
  [owner context-entities]
  [:div.literal-context
   (for [{:keys [datum/id datum/label fact/attribute]} context-entities]
     [:div.literal-context-datum {:react-key label}
      [:a {:href (when id (browser/route-for owner :datum :id id))}
       [:div.context-entity-label
        [:span label]]
       [:div.context-entity-fact
        [:div.mini-fact-handle
         [:svg {:width "12px" :height "12px"}
          [:g.handle-borders
           {:transform "translate(2, 2)"}
           [:path {:d "m 0 4 v -4 h 4"
                   :stroke "#666"
                   :fill "transparent"}]
           [:path {:d "m 7 4 v 4 h -4"
                   :stroke "#666"
                   :fill "transparent"}]]]]
        [:div.context-entity-attribute
         [:span.context-entity-attribute attribute]]]]])])

(defn literal-types
  "Triggers for switching the current literal's type."
  [literal owner current-type available-types]
  (let [type-list 
        (->> available-types
             (remove #(contains? #{(:ident current-type) :datum/label} (:ident %)))
             (cons current-type)) 
        ]
    [:div.literal-types
     (for [{:keys [ident label] :as tp} type-list
           :let [current? (= (:ident current-type) ident)]]
       [:div.literal-type-container {:react-key ident}
        [:a.available-literal-type
         {:on-click
          (fn [_]
            (->> (msg/update-literal (:db/id literal) ident (schema/get-value literal))
                 (msg/send! owner)))
          :class [(when current? "current")]}
         [:span label]]])]))

(defn container
  "The whole page. Splitting it into three columns. I might allow the
  side columns to fade out when the user is in the zone, but I don't
  want them to move or change size."
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "literal-" (:db/id props)))

    om/IRender
    (render [_]
      (let [type-ordering (get schema/input-types-ordered-by-fact-part-preference
                               :fact/attribute)
            available-types (->> props :literal
                                 schema/get-value
                                 schema/compatible-input-types
                                 (util/sort-by-ordering :ident type-ordering))
            current-type (schema/get-input-type (:literal props))
            ]
        (html
         [:div.literal-container.container-fluid
          [:div.row
           ; TODO: consider adjusted design for narrow screens.
           [:div.col-md-2.literal-context
            (literal-context owner (get props :context [{:datum/label "Test label"
                                                         :fact/attribute "the attribute oh yes it's the attribute"}]))]
           [:div.col-md-8.literal-content
            (literal-editor owner (get props :literal))
            (when (contains? schema/renderable-types (get current-type :ident))
              (literal-renderer owner (get props :literal)))]

           [:div.col-md-2.literal-types
            (literal-types (get props :literal) owner current-type available-types)]]

          ])))))

