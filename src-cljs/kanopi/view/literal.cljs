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

(defn literal-context [owner context-entities])
(defn literal-types [owner available-types])

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
      (println "RENDER LITERAL")
      (let [available-types []]
        (html
         [:div.literal-container.container-fluid
          [:div.row
           [:div.col-md-2.literal-context
            (literal-context owner (get props :context))]
           [:div.col-md-8.literal-content
            (literal-editor owner (get props :literal))]
           [:div.col-md-2.literal-types
            (literal-types owner available-types)]]
          
          ])))))

