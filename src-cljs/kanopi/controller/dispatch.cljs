(ns kanopi.controller.dispatch
  "Route messages traveling in the aether to local event handlers
  and/or spouts for external processing."
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [quile.component :as component]
            [cljs.core.async :as async]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.aether.core :as aether]
            [kanopi.controller.handlers :as handlers]
            [kanopi.model.message :as msg]
            [om.core :as om]
            ))

(def mode-verbs
  {:demo
   {:local  #{:navigate :search :update-thunk-label :update-fact}
    :remote #{}}

   :authenticated
   {:local  #{:navigate :search} 
    :remote #{:update-thunk-label :update-fact}}})

(defrecord Dispatcher [config aether app-state kill-channel]
  component/Lifecycle
  (start [this]
    (info "start dispatcher")
    (let [kill-ch  (async/chan 1)
          listener (aether/replicate! (get aether :aether))]
      (asyncm/go (loop [[v ch] nil]
                   (if (= ch kill-ch)
                     (do
                      (async/close! kill-ch))
                     (let [{:keys [noun verb context]} v
                           root-crsr    (om/root-cursor (:app-state app-state))
                           mode         (get @root-crsr :mode)
                           local-verbs  (get-in mode-verbs [mode :local])
                           remote-verbs (get-in mode-verbs [mode :remote])]
                       ;; first run v is nil
                       (when v
                         (when (contains? local-verbs verb)
                           (handlers/local-event-handler root-crsr v))
                         (when (contains? remote-verbs verb)
                           (->> v
                                (msg/local->remote root-crsr)
                                (async/put! (get-in aether [:aether :publisher])))))

                       (recur (async/alts! [listener kill-ch]))))))
      (assoc this :kill-channel kill-ch)))

  (stop [this]
    (async/put! kill-channel :kill)
    (assoc this :kill-channel nil)))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->Dispatcher {:config config})))
