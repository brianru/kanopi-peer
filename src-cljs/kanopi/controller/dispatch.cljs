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
  {:spa.unauthenticated/online
   {:local  #{
              :spa/navigate
              :spa.navigate/search

              :spa.login/success    :spa.login/failure
              :spa.logout/success   :spa.logout/failure
              :spa.register/success :spa.register/failure

              :datum/create
              :datum/get
              :datum.fact/add
              :datum.fact/update
              :datum.label/update

              ;; :goal.modal/open
              ;; :goal/create

              ;; :insight.modal/open
              ;; :insight/record

              ;; NOTE: initialize-client-state* is not here
              }
    :remote #{
              :spa/login :spa/logout :spa/register
              }}

   :spa.authenticated/online
   {:local  #{
              :spa/navigate

              :spa.login/success    :spa.login/failure
              :spa.logout/success   :spa.logout/failure
              :spa.register/success :spa.register/failure

              :spa/switch-team

              :spa.navigate.search/success     :spa.navigate.search/failure

              :datum.create/success            :datum.create/failure
              :datum.get/success               :datum.get/failure
              :datum.fact.add/success          :datum.fact.add/failure
              :datum.fact.update/success       :datum.fact.update/failure
              :datum.label.update/success      :datum.label.update/failure
              :spa.state.initialize/success    :spa.state.initialize/failure
              } 
    :remote #{
              :spa/login :spa/logout :spa/register

              :spa.navigate/search

              :datum/create
              :datum/get
              :datum.fact/add
              :datum.fact/update
              :datum.label/update
              :spa.state/initialize

              }}})

(defrecord Dispatcher [config aether history app-state kill-channel]
  component/Lifecycle
  (start [this]
    (info "start dispatcher" (get config :mode))
    (let [kill-ch  (async/chan 1)
          listener (aether/replicate! aether)]
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
                         ;; NOTE: a verb can belong to both
                         ;; local-verbs and remote-verbs.
                         ;; In that case the message is handled twice:
                         ;; once locally, once remotely.
                         (when (contains? local-verbs verb)
                           (handlers/local-event-handler aether history root-crsr v))
                         (when (contains? remote-verbs verb)
                           (->> v
                                (msg/local->remote history root-crsr)
                                (aether/send! aether))))

                       (recur (async/alts! [listener kill-ch]))))))
      (assoc this :kill-channel kill-ch)))

  (stop [this]
    (async/put! kill-channel :kill)
    (assoc this :kill-channel nil)))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->Dispatcher {:config config})))
