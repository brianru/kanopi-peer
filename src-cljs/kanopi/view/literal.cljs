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

            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]

            [kanopi.util.core :as util]
            ))

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
    (println "LITERAL EDITOR" literal)
    (some-> (schema/get-input-type literal)
            (get :ident)
            (name)
            (keyword))
    :code)
  :default :code)

(defmethod literal-editor :code
  [owner literal]
  (om/build text-editor/code literal))

(defmethod literal-editor :math
  [owner literal]
  )

(comment
 
 ; https://github.com/Khan/KaTeX
 (defmethod literal-editor :math
   []
   )
 
 (defmethod literal-editor :markdown
   []
   )
 )

(defn literal-context
  "A vertical list of somewhat stylized relating entities. Each is a
  link to that entity and maybe hovering renders a popover with more
  information. The font size is relatively small."
  [owner context-entities]
  [:div.literal-context
   (for [{:keys [datum/label fact/attribute]} context-entities]
     [:div {:react-key label}
      [:a {:href nil}
       [:div.context-entity-label
        [:span label]]
       [:div.context-entity-fact
        [:span.handle]
        [:span.context-entity-attribute
         attribute]]]
      ])])

(defn literal-types
  ""
  [owner current-type available-types]
  [:div.literal-types
   (for [{:keys [ident label] :as tp} available-types
         :let [current? (= (:ident current-type) ident)]]
     [:div {:react-key ident}
      [:a.available-literal-type
       {:href nil
        :class [(when current? "current")]}
       [:span label]]])])

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
      (println "RENDER LITERAL" props)
      (let [type-ordering (get schema/input-types-ordered-by-fact-part-preference :fact/attribute)
            available-types (->> props
                                 schema/get-value
                                 schema/compatible-input-types
                                 (util/sort-by-ordering :ident type-ordering))
            current-type (schema/get-input-type props)]
        (html
         [:div.literal-container.container-fluid
          [:div.row
           ; TODO: consider adjusted design for narrow screens.
           [:div.col-md-2.literal-context
            (literal-context owner (get props :context [{:datum/label "Test label"
                                                         :fact/attribute "the attribute"}]))]
           [:div.col-md-8.literal-content
            (literal-editor owner (get props :literal))]
           [:div.col-md-2.literal-types
            (literal-types owner current-type available-types)]]
          
          ])))))

