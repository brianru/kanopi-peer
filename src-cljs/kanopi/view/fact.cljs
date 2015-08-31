(ns kanopi.view.fact
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(defn handle
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-handle-" (:id props)))

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let []
        (html
         [:div.fact-handle
          [:div.fact-handle-top
           [:div.fact-handle-top-left]
           [:div.fact-handle-top-right]]
          [:div.fact-handle-bottom
           [:div.fact-handle-bottom-left]
           [:div.fact-handle-bottom-right]]
          [:div.fact-handle-icon]
          ])))
    ))

(defn attribute
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-attribute-" (:db/id props)))

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let [_ (println props)]
        (html
         [:div.fact-attribute
          ])))
    ))

(defn value
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-value-" (:id props)))

    om/IRenderState
    (render-state [_ {:keys [mode] :as state}]
      (let []
        (html
         [:div.fact-value
          ])))
    ))

(defn body
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact-body-" (:id props)))

    om/IInitState
    (init-state [_]
      {:mode :read})

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.fact-body
          [:div.inline-10-percent
           (om/build handle props {:state (select-keys state [:mode])})]

          [:div.inline-90-percent
           (om/build attribute props {:state (select-keys state [:mode])})
           (om/build value     props {:state (select-keys state [:mode])})]
          ])))
    ))

(defn container
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "fact" (:db/id props)))
    om/IRender
    (render [_]
      (let []
        (html
         [:div.fact-container
          (om/build body props)])))
    ))
