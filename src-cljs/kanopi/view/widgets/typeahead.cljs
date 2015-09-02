(ns kanopi.view.widgets.typeahead
  "What do I want from typeahead search?
  
  "
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [sablono.core :refer-macros [html] :include-macros true]
            [cljs.core.async :as async]
            [kanopi.view.widgets.dropdown :as dropdown]
            [kanopi.util.async :as async-util]
            [kanopi.ether.core :as ether]))

(defn typeahead
  "Pre-load with local or cached results.
  "
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:handler-fn (constantly nil)
       :input-ch (-> (async/ch) (async-util/debounce 250))
       :kill-ch (async/ch)
       :results {;; <search-term>
                 ;; [<results>]]
                 }
       })

    om/IWillMount
    (will-mount [_]
      (let [{:keys [input-ch kill-ch]} (om/get-state owner)]
        (asyncm/go
         (loop [[v ch] nil]
           (if (= ch kill-ch)
             (do
              (doseq [ch [kill-ch input-ch]]
                (async/close! ch)))
             (do
              (async/>! (om/get-shared owner [:ether :publisher])
                        (msg/fulltext-search owner v))
              (recur (async/alts! [result-ch kill-ch]))))
           )))
      (ether/listen! owner :verb :fulltext-search-results
                     (fn [{:keys [noun verb context]}]
                       )))

    om/IWillUnmount
    (will-unmount [_]
      (async/put! (om/get-state owner :kill-ch) :stop!))

    om/IRenderState
    (render-state [_ state]
      (let []
        (html
         [:div.typeahead
          [:input
           {:type      "text"
            :on-change #(let [v (.. % -target -value)]
                         (async/put! (get state :input-ch) v)
                         (om/set-state! owner :input-value v)) 
            :value     (get state :input-value)}]
          ])))))
