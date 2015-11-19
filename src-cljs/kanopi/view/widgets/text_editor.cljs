(ns kanopi.view.widgets.text-editor
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros (html) :include-macros true]
            cljsjs.codemirror

            [kanopi.model.message :as msg]
            [kanopi.util.browser :as browser]
            ))

(defn rich-text
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "rich-text-editor-" props))))

(defn code
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "code-editor-" props))
    
    om/IDidMount
    (did-mount [_]
      (.fromTextArea js/CodeMirror
                     (om/get-node owner)
                     #js {:mode "clojure"
                          :lineNumbers false}))
    
    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:textarea
          ])))))
