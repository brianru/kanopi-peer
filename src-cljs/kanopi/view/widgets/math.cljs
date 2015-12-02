(ns kanopi.view.widgets.math
  "I do not like the name of this namespace. Not sure if there should
  be a namespace for rendering all sorts of stuff, but for now all I
  have are these math-as-latex literals."
  (:require [om.core :as om]
            [sablono.core :refer-macros (html)]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            ))

(def ^:private katex-opts
  {})

(defn- render-katex!
  ([owner]
   (render-katex! owner katex-opts))
  ([owner opts]
   (when-let [v (om/get-state owner :input-value)]
    (. js/katex render v (om/get-node owner) (clj->js katex-opts)))))

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
    
    om/IRender
    (render [_]
      (html
         [:div.katex-container]))))
