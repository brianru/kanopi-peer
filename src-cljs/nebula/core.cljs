(ns nebula.core
  (:require [om.core :as om :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [shodan.console :refer-macros [log] :include-macros true]
            ))

(def app-container (. js/document (getElementById "app-container")))
(def app-state
  (atom {:tempo {:pulse nil}
         :user  {:actions {}}
         :data  {}}))
(defn root
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "app-root")

    om/IRender
    (render [_]
      (html
       [:div
        [:h1 "Hi!"]]
       ))))

(om/root root app-state
         {:target app-container
          :shared {}})
