(ns kanopi.view.thunk
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [shodan.console :refer-macros [log] :include-macros true]
            [kanopi.view.fact :as fact]
            ))

(defn node-link [node]
  (let []
    [:div.node-link
     [:a {:href (:url node)}
      (:label node)]]
    ))

(defn parents
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-parents" (:id props)))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.thunk-parents
          (for [node (:parents props)]
            (node-link node))])))
    ))

(defn body
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-body" (:id props)))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.thunk-body
          [:div.thunk-title
           ]
          [:div.thunk-facts
           (for [f (:facts props)]
             (om/build fact/container f))]
          ])))
    ))

(defn children
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-children" (:id props)))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.thunk-children
          (for [node (:children props)]
            (node-link node))])))
    ))

(defn container
  ""
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk" (:id props)))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.thunk-container
          (om/build parents  props)
          (om/build body     props)
          (om/build children props)])))
    ))
