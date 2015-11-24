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

(defn- handle-change [e owner korks]
  (om/set-state! owner korks (.. e -target -value)))

(defn- end-edit [e owner handlerfn]
  (handlerfn (.. e -target -value))
  (om/set-state! owner :new-value nil))

(defn rich-text
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "rich-text-editor-" props))
    
    om/IInitState
    (init-state [_]
      {:placeholder nil
       :tab-index 0 
       :new-value nil 
       :on-submit (constantly nil)}
      )
    om/IDidMount
    (did-mount [_]
      #_(.fromTextArea js/ProseMirror
                       (om/get-node owner)
                       ))

    om/IRenderState
    (render-state [_ {:keys [edit-key new-value on-submit] :as state}]
      (let [current-value (or new-value (get props edit-key))]
        (html
         [:textarea
          ; TODO: save periodically, without the user exiting.
          {:value current-value
           :placeholder (get state :placeholder)
           :tab-index   (get state :tab-index)
           :on-change   #(handle-change % owner :new-value)
           :on-key-down #(when (= (.-key %) "Enter")
                           (.blur (.-target %)))
           :on-blur     #(end-edit % owner on-submit)}])))))

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

    om/IWillUnmount
    (will-unmount [_]
      ; removes CodeMirror from DOM
      (.toTextArea (om/get-state owner :editor)))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:textarea
          {:default-value ""}
          ])))))
