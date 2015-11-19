(ns kanopi.view.literal
  "TODO for literal view/edit support:
  ;; 1 - stub component
  ;; 2 - routing
  ;; 3 - modify all links to check and use :datum vs :literal as appropriate
  4 - where do I want to store current-literal data in app-state? 
  5 - header, intent, breadcrumbs
  6 - 

  What does the user need to know to edit a literal?
  - datum + fact[attribute] => fact[literal]
  - other compatible types
  - all other types, if the user wants.
  - history of changes (AND contributors)

  Related verbs:
  - :literal/get
  - :literal/update

  "
  (:require [om.core :as om]
            [schema.core :as s]
            [sablono.core :refer-macros (html) :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]

            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]
            [cljsjs.codemirror :as codemirror]
            ))

(defmulti literal-editor
  (fn [literal _ _]
    (println "LITERAL EDITOR" literal)
    (-> (schema/get-input-type literal)
        (name)
        (keyword)))
  :default :text)

; Medium-style text editor?
(defmethod literal-editor :text
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IDidMount
    (did-mount [_]
      (println "did mount" js/CodeMirror))
    
    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:textarea
          ])))))

(defmethod literal-editor :math
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {})
    
    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div
          ])))))

; 
(defmethod literal-editor :integer
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {})
    
    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div
          ])))))

(comment
 
 ; https://github.com/Khan/KaTeX
 (defmethod literal-editor :math
   []
   )
 
 ; http://ionicabizau.github.io/medium-editor-markdown/
 (defmethod literal-editor :markdown
   []
   )
 )

; etc...

(defn container
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "literal-" (get-in props [:literal])))
    
    om/IRender
    (render [_]
      (let []
        (html
         [:div.literal-container.container-fluid
          (om/build literal-editor props)
          ]))))
  )

