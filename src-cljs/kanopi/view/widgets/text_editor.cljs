(ns kanopi.view.widgets.text-editor
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros (html) :include-macros true]
            ; cljsjs.codemirror

            [kanopi.model.message :as msg]
            [kanopi.util.browser :as browser]

            ; https://github.com/cljsjs/packages/tree/master/codemirror#modes
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.codemirror.mode.stex
            ))

(defn- change-handler [owner e]
  (let [on-change (om/get-state owner :on-change)]
    (on-change (.getValue e))))

(defn- blur-handler [owner e]
  (let [{:keys [on-submit document]} (om/get-state owner)]
    (on-submit (.getValue document))))

(defn- register-event-handlers!
  [editor owner]
  (doto editor
    (.on "change"     (partial change-handler owner))
    (.on "blur"       (partial blur-handler owner))))

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
         [:textarea.text-editor.rich-text-editor
          ; TODO: save periodically, without the user exiting.
          {:value current-value
           :placeholder (get state :placeholder)
           :tab-index   (get state :tab-index)
           :on-change   #(handle-change % owner :new-value)
           :on-key-down #(when (= (.-key %) "Escape")
                           (.blur (.-target %)))
           :on-blur     #(end-edit % owner on-submit)}])))))

(defn code
  "NOTE: Root dom node must be a textarea for simple mounting of
  CodeMirror editor. See fromTextArea call in did-mount, particularly
  the call to om/get-node."
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "code-editor-" props))

    om/IInitState
    (init-state [_]
      {
       })
    
    om/IDidMount
    (did-mount [_]
      (let [state  (om/get-state owner)
            editor (.fromTextArea js/CodeMirror
                                  (om/get-node owner)
                                  #js {:lineNumbers true
                                       :mode (get state :language-mode)})
            doc    (.getDoc editor)]
        (register-event-handlers! editor owner)
        (om/update-state! owner #(assoc % :editor editor :document doc))))

    om/IWillUnmount
    (will-unmount [_]
      ; removes CodeMirror from DOM
      (when-let [editor (om/get-state owner :editor)]
        (.toTextArea editor)))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:textarea.text-editor.code-text-editor
          {:default-value ""}
          ])))))

(def ^:private required-code-editor-args
  [:on-change :on-submit :edit-key])
(def ^:private optional-code-editor-args
  [:on-change :placeholder :tab-index :input-type :keymap])

(defn input-type->language-mode
  "Fn because I think this mapping should be elsewhere, maybe more complex.
  I feel weird about putting this in schema.cljc because it's specific
  to CodeMirror."
  [tp]
  (get {:literal/math "stex"} tp))

(defn code-editor-config*
  [required-args optional-args]
  (let [{:keys [edit-key input-type on-change on-submit]}
        required-args]
    (hash-map
     :state      {}
     :init-state (merge optional-args
                        {:edit-key      edit-key
                         :language-mode (input-type->language-mode input-type)
                         :on-change     on-change
                         :on-submit     on-submit}) 
     )))

(defn code-editor-config [& args]
  (let [argmap (apply hash-map args)]
    (code-editor-config*
      (select-keys argmap required-code-editor-args)
      (select-keys argmap optional-code-editor-args))))
