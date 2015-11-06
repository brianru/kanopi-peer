(ns kanopi.view.datum
  (:require [om.core :as om :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.view.fact :as fact]
            [cljs.core.async :as async]
            [ajax.core :as http]
            [kanopi.util.browser :as browser]
            [kanopi.model.message :as msg]
            [kanopi.model.schema :as schema]
            [kanopi.view.widgets.input-field :as input]
            ))

(defn context-datums
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "datum-parents"))

    om/IInitState
    (init-state [_]
      {:expand false})

    om/IRenderState
    (render-state [_ state]
      (let [size 9]
        (html
         [:div.row.context-datums
           (for [[idx datum] (->> props (map-indexed vector)
                                  (take (if (get state :expand)
                                          size
                                          (js/Math.sqrt size))))
                 :when (:db/id datum)]
             [:div.context-datum-cell.col-xs-3.col-md-3.vcenter
              {:key (:db/id datum)
               :style (cond-> {}
                        (= 0 idx)
                        (merge {:margin-left "11%"}))}
              [:a {:href (browser/route-for owner :datum :id (:db/id datum))}
               [:span (:datum/label datum)]]])
          ])))))

(defn body
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "datum-body"))

    om/IRenderState
    (render-state [_ state]
      (let [fact-count (count (get props :datum/fact))]
        (html
         [:div.row
          [:div.datum-body.col-xs-offset-1.col-md-offset-3.col-xs-10.col-md-6
           [:div.datum-label
            (om/build input/editable-value props
                      {:init-state {:editing (get state :editing-label)
                                    :edit-key :datum/label
                                    :submit-value
                                    (fn [label']
                                      (->> label'
                                           (msg/update-datum-label (om/get-props owner))
                                           (msg/send! owner)))}})
            ]
           [:div.datum-facts
            ;; NOTE: I want to actually group fact by attribute and
            ;; render in those groups, but that does not work with
            ;; Om's cursor model. TODO: make change when I upgrade to
            ;; Om Next.
            (om/build-all fact/fact-next
                          (get props :datum/fact [])
                          {:key-fn :db/id
                           :init-state {:datum-id (:db/id props)}
                           :state {:fact-count fact-count}})
            ;; NOTE: also build a special fact which is the trigger to
            ;; add new facts
            (om/build fact/fact-next {}
                      {:react-key "nil-fact"
                       :init-state {:datum-id (:db/id props)}
                       :state {:fact-count fact-count}})
            ]]])))
    ))

(defn similar-datums
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "datum-similar-datums"))

    om/IInitState
    (init-state [_]
      {:expand false})

    om/IRenderState
    (render-state [_ state]
      (let [size 9]
        (html
         [:div.similar-datums.row
          (for [[idx datum] (->> props (map-indexed vector)
                                 (take (if (get state :expand)
                                         size
                                         (js/Math.sqrt size))))
                :when (:db/id datum)]
            [:div.similar-datum-cell.vcenter.col-xs-3.col-md-3
             {:key (:db/id datum)
              :style (cond-> {}
                       (= 0 idx)
                       (merge {:margin-left "11%"}))}
             [:a {:href (browser/route-for owner :datum :id (:db/id datum))}
              [:span (:datum/label datum)]]]
            )
          ])))))

(defn container
  ""
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "datum" (get-in props [:datum :db/id])))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.datum-container.container-fluid
          (om/build context-datums (get props :context-datums))
          (om/build body (get props :datum))
          (om/build similar-datums (get props :similar-datums))
          ])))
    ))
