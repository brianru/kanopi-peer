(ns kanopi.view.thunk
  (:require [om.core :as om :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.view.fact :as fact]
            [cljs.core.async :as async]
            [ajax.core :as http]
            ))

(defn node-link [node]
  (let []
    [:div.node-link
     [:a {:href (:url node)}
      (:label node)]]
    ))

(defn context-entities
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

(defn similar-entities
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-similar-entities" (:id props)))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.thunk-similar-entities
          (for [node (:similar-entities props)]
            (node-link node))])))
    ))

(defn container
  ""
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk" (:id props)))

    om/IWillMount
    (will-mount [_]
      ;; TODO: make sure I have the current thunk in app-state
      ;; get current thunk id from uri
      )

    om/IRender
    (render [_]
      (let [
           ;; _ (async/put! (om/get-shared owner [:ether :publisher])
           ;;               {:noun nil
           ;;                :verb :request
           ;;                :context nil})
            resp (http/GET "/api/" {:params {:ent-id 17592186045429
                                             :noun 17592186045429
                                             :verb :request
                                             :context {:source :web}}} )
            _ (info resp)]
        (html
         [:div.thunk-container
          [:h1 "Thunk"]
          ;(om/build context-entities  (get props :context-entities)
          ;(om/build body     (get props :thunk)
          ;(om/build similar-entities (get props :similar-entities)
          ])))
    ))
