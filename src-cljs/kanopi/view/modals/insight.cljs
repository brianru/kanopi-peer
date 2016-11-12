(ns kanopi.view.modals.insight
  (:require [om.core :as om]
            [schema.core :as s]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.model.schema :as schema]
            [kanopi.model.message :as msg]
            [kanopi.view.widgets.modal :as modal]
            [kanopi.util.core :as util]))

(defn capture
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "insight-capture-modal")

    om/IInitState
    (init-state [_]
      {})

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         (modal/modal-template {}))))))
