(ns kanopi.view.widgets.typeahead
  "What do I want from typeahead search?
  
  "
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]))

(defn typeahead
  "Pre-load with local or cached results.
  "
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:handler-fn (constantly nil)
       :results {;; <search-term>
                 ;; [<results>]]
                 }
       })

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.typeahead
          ])))))
