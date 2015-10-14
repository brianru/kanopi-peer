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

(defn- corner?
  ([w h n]
   (case [(mod n w) (mod n h) (= (* w h) n)]
     [1 1 false] :top-left
     [0 0 false] :top-right
     [1 0 false] :bottom-left
     [0 0 true]  :bottom-right
     ;; default 
     nil))
  ([size n]
   (let [x (js/Math.sqrt size)]
     (corner? x x n))))

(defn- corner-styling [size n]
  (let [corner (corner? size n)]
    (case false ;;corner
      :top-left
      {:border-top "1px solid black"
       :border-left "1px solid black"}

      :top-right
      {:border-top "1px solid black"
       :border-right "1px solid black"}

      :bottom-left
      {:border-bottom "1px solid black"
       :border-left "1px solid black"}

      :bottom-right
      {:border-bottom "1px solid black"
       :border-right "1px solid black"}

      ;;default
      {})))

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
                 :let []
                 :when (:db/id datum)]
             [:div.context-datum-cell.col-xs-3.col-md-3.vcenter
              {:style (cond-> {}
                        true
                        (merge (corner-styling size (inc idx)))
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

    om/IRender
    (render [_]
      (let []
        (html
         [:div.row
          [:div.datum-body.col-xs-offset-1.col-md-offset-3.col-xs-10.col-md-6
           [:div.datum-title
            (om/build input/editable-value props
                      {:init-state {:edit-key :datum/label
                                    :submit-value
                                    (fn [label']
                                      (->> label'
                                           (msg/update-datum-label (om/get-props owner))
                                           (msg/send! owner)))}})
            ]
           [:div.datum-type-line
            ;[:span.horizontal-line]
            ;[:span.datum-type-label "TYPE: "]
            ;[:span.datum-type (name (schema/describe-entity props))]
            ;[:span.horizontal-line]
            ]
           [:div.datum-facts
            ;; NOTE: one of the facts is a placeholder for creating a
            ;; new one
            (if (:db/id props)
              (om/build-all fact/container
                            (get props :datum/fact)
                            {:key-fn :db/id
                             :init-state {:datum-id (:db/id props)}
                             :state {:fact-count (count (:datum/fact props))}})
              [:span "This datum does not exist."])]
           ]
          ])))
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
             {:style (cond-> {}
                       true
                       (merge (corner-styling size (inc idx)))
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
