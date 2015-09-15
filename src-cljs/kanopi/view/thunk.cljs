(ns kanopi.view.thunk
  (:require [om.core :as om :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.view.fact :as fact]
            [cljs.core.async :as async]
            [ajax.core :as http]
            [kanopi.util.browser :as browser]
            [kanopi.model.message :as msg]
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

(defn context-thunks
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-parents"))

    om/IInitState
    (init-state [_]
      {:expand false})

    om/IRenderState
    (render-state [_ state]
      (let [size 9]
        (html
         [:div.row.context-thunks
           (for [[idx thunk] (->> props (map-indexed vector)
                                  (take (if (get state :expand)
                                          size
                                          (js/Math.sqrt size))))
                 :let []
                 :when (:db/id thunk)]
             [:div.context-thunk-cell.col-xs-3.col-md-3.vcenter
              {:style (cond-> {}
                        true
                        (merge (corner-styling size (inc idx)))
                        (= 0 idx)
                        (merge {:margin-left "11%"}))}
              [:a {:href (browser/route-for owner :thunk :id (:db/id thunk))}
               [:span (:thunk/label thunk)]]])
          ])))))

(defn body
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-body"))

    om/IWillUpdate
    (will-update [this next-props next-state]
      (println "will-update" next-props))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.row
          [:div.thunk-body.col-xs-offset-1.col-md-offset-3.col-xs-10.col-md-6
           [:div.thunk-title
            (om/build input/editable-value props
                      {:init-state {:edit-key :thunk/label
                                    :submit-value
                                    (fn [label']
                                      (->> (assoc (om/get-props owner) :thunk/label label')
                                           (msg/update-entity-value (om/get-props owner))
                                           (msg/send! owner)))}})
            ;;[:h1 (get props :thunk/label)]
            ]
           [:div.thunk-facts
            (for [f (:thunk/fact props)]
              (om/build fact/container f {:key-fn :db/id}))
            (om/build fact/new-fact props)]
           ]
          ])))
    ))

(defn similar-thunks
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk-similar-thunks"))

    om/IInitState
    (init-state [_]
      {:expand false})

    om/IRenderState
    (render-state [_ state]
      (let [size 9]
        (html
         [:div.similar-thunks.row
          (for [[idx thunk] (->> props (map-indexed vector)
                                 (take (if (get state :expand)
                                         size
                                         (js/Math.sqrt size))))
                :let [_ (println thunk)]
                :when (:db/id thunk)]
            [:div.similar-thunk-cell.vcenter.col-xs-3.col-md-3
             {:style (cond-> {}
                       true
                       (merge (corner-styling size (inc idx)))
                       (= 0 idx)
                       (merge {:margin-left "11%"}))}
             [:a {:href (browser/route-for owner :thunk :id (:db/id thunk))}
              [:span (:thunk/label thunk)]]]
            )
          ])))))

(defn container
  ""
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "thunk" (get-in props [:thunk :db/id])))

    om/IRender
    (render [_]
      (let []
        (html
         [:div.thunk-container.container-fluid
          (om/build context-thunks (get props :context-thunks))
          (om/build body (get props :thunk))
          (om/build similar-thunks (get props :similar-thunks))
          ])))
    ))
