(ns kanopi.controller.dispatch
  "Route messages traveling in the ether to local event handlers
  and/or spouts for external processing."
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [quile.component :as component]
            [cljs.core.async :as async]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.controller.handlers :as handlers]
            [kanopi.ether.core :as ether]))

(defrecord Dispatcher [config ether app-state kill-channels]
  component/Lifecycle
  (start [this]
    (let [target-verbs [:navigate]
          _ (info "start dispatcher" target-verbs)
          kill-chs
          (doseq [vrb target-verbs]
            (ether/listen*
             (:ether ether) :verb vrb
             {:handlerfn (partial handlers/local-event-handler (:app-state app-state))
              :logfn (constantly nil)}))
          ]
      (assoc this :kill-channels kill-chs)))

  (stop [this]
    (doseq [ch kill-channels]
      (async/put! ch :kill))
    (assoc this :kill-channels nil)))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->Dispatcher {:config config})))
