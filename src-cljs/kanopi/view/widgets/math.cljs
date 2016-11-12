(ns kanopi.view.widgets.math
  "I do not like the name of this namespace. Not sure if there should
  be a namespace for rendering all sorts of stuff, but for now all I
  have are these math-as-latex literals.

  TODO: consider switching to mathjax for broader input support."
  (:require [om.core :as om]
            [sablono.core :refer-macros (html)]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(def ^:private katex-opts
  {"displayMode" true
   "throwOnError" true})

(defn- render-katex!
  ([owner]
   (render-katex! owner katex-opts))
  ([owner opts]
   (when-let [v (om/get-state owner :input-value)]
     (try
      ; first call throws any errors without affectign the DOM,
      ; second call pushes stuff to dom. we are wastefully rendering
      ; the input twice, we could save the output of renderToString
      ; and stick that in the DOM. If katex is doing any caching this
      ; isn't a problem, and I like the clarity of using `render`
      ; instead of hitting the DOM myself.
      (. js/katex renderToString v)
      (. js/katex render v (om/get-node owner) (clj->js katex-opts))
      (om/set-state! owner :parse-error nil)
      (catch js/Object e
        (om/set-state! owner :parse-error (.-message e))))
     )))

(defn katex
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "katex-renderer")
    om/IInitState
    (init-state [_]
      {:input-value nil})

    om/IDidMount
    (did-mount [_]
      (render-katex! owner))

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (when (not= (get prev-state :input-value) (om/get-state owner :input-value))
        (render-katex! owner)))

    om/IRenderState
    (render-state [_ {:keys [parse-error] :as state}]
      (html
       [:div.katex-container
        ; TODO: better error msg styling.
        ; NOTE: katex inserts rendered value as last child of :div.katex-container
        (when parse-error
          [:span.katex-error parse-error])]))))
