(ns kanopi.core
  (:require [om.core :as om :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.view.fact :as fact]
            ))

(def app-container (. js/document (getElementById "app-container")))

(def app-state
  (atom {:tempo {:pulse nil}
         :user  {
                 :actions []
                 }
         :thunk {}
        }))

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

(defn mount-root! []
  (om/root root app-state
           {:target app-container
            :shared {}}))

(mount-root!)
