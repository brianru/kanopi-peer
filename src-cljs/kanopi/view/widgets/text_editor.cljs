(ns kanopi.view.widgets.text-editor
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros (html) :include-macros true]
            ; cljsjs.codemirror

            [kanopi.model.message :as msg]
            [kanopi.util.browser :as browser]
            ))

(defn- on-change [state e]
  (println "change event" e)
  #_(on-submit (.getValue e))
  )

(defn- key-handled [state e]
  (println "keyHandled event" e))

(defn- blur [state e]
  (println "blur event" e))

(defn- register-event-handlers!
  [editor {:keys [on-submit] :as state}]
  (doto editor
    (.on "change"     (partial on-change state))
    (.on "keyHandled" (partial key-handled state))
    (.on "blur"       (partial blur state))))

(defn rich-text
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "rich-text-editor-" props))
    
    om/IDidMount
    (did-mount [_]
      #_(.fromTextArea js/ProseMirror
                       (om/get-node owner)
                       ))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:textarea
          {}])))))

(defn code
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "code-editor-" props))
    
    om/IDidMount
    (did-mount [_]
      (let [editor (.fromTextArea js/CodeMirror
                                  (om/get-node owner)
                                  #js {:lineNumbers true})]
        (register-event-handlers! editor (om/get-state owner))
        (om/set-state! owner :editor editor)))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:textarea
          {:default-value ""}
          ])))))
