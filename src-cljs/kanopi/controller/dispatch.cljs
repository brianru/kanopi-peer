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
    ;; TODO: How should I inject the local and remote verbs into this
    ;; component? How can this get me closer to a system that can
    ;; seamlessly shift between offline and online, demo to
    ;; authenticated modes?
    (let [
          local-verbs [:navigate :search]
          remote-verbs []
          _ (info "start dispatcher" local-verbs remote-verbs)

          local-kill-chs
          (doseq [vrb local-verbs]
            (ether/listen*
             (:ether ether) :verb vrb
             {:handlerfn (partial handlers/local-event-handler (:app-state app-state))})
            )
          remote-kill-chs
          (doseq [vrb remote-verbs]
            )

          ]
      (assoc this :kill-channels (concat local-kill-chs remote-kill-chs))))

  (stop [this]
    (doseq [ch kill-channels]
      (async/put! ch :kill))
    (assoc this :kill-channels nil)))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->Dispatcher {:config config})))
