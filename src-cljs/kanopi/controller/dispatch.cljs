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
            [kanopi.controller.handlers.request :as request-handlers]
            [kanopi.controller.handlers.response :as response-handlers]
            [kanopi.model.message :as msg]
            [kanopi.model.message.client :as client-msg]
            [om.core :as om]
            ))

(def mode-verbs
  {:spa.unauthenticated/online
   {:local  {:request
             #{:spa/navigate
               :spa.navigate/search
               :datum/create
               :datum/get
               :datum.fact/add
               :datum.fact/update
               :datum.label/update
               }
             :response
             #{
               :spa.login/success    :spa.login/failure
               :spa.logout/success   :spa.logout/failure
               :spa.register/success :spa.register/failure

               :spa.navigate.search/success     :spa.navigate.search/failure

               :datum.create/success            :datum.create/failure
               :datum.get/success               :datum.get/failure
               :datum.fact.add/success          :datum.fact.add/failure
               :datum.fact.update/success       :datum.fact.update/failure
               :datum.label.update/success      :datum.label.update/failure
               }}
    :remote {:request
             #{:spa/login :spa/logout :spa/register
               }
             :response
             #{}}}

   :spa.authenticated/online
   {:local  {:request
             #{:spa/navigate
               :spa/switch-team
               }
             :response
             #{:spa.login/success    :spa.login/failure
               :spa.logout/success   :spa.logout/failure
               :spa.register/success :spa.register/failure


               :spa.navigate.search/success     :spa.navigate.search/failure

               :datum.create/success            :datum.create/failure
               :datum.get/success               :datum.get/failure
               :datum.fact.add/success          :datum.fact.add/failure
               :datum.fact.update/success       :datum.fact.update/failure
               :datum.label.update/success      :datum.label.update/failure
               :spa.state.initialize/success    :spa.state.initialize/failure
               }}
    :remote {:request
             #{
               :spa/login :spa/logout :spa/register

               :spa.navigate/search

               :datum/create
               :datum/get
               :datum.fact/add
               :datum.fact/update
               :datum.label/update
               :spa.state/initialize
               }
             :response
             #{}
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
                           local-request-verbs  (get-in mode-verbs [mode :local :request])
                           local-response-verbs (get-in mode-verbs [mode :local :response])
                           remote-request-verbs (get-in mode-verbs [mode :remote :request])]
                       ;; first run v is nil
                       ;; TODO: considerably more sophisticated
                       ;; exception handling. at least log them!
                       (when v
                         ;; NOTE: a verb can belong to both
                         ;; local-verbs and remote-verbs.
                         ;; In that case the message is handled twice:
                         ;; once locally, once remotely.
                         (when (contains? local-request-verbs verb)
                           (try
                            (request-handlers/local-request-handler
                             aether history root-crsr v)
                            (catch js/Object e
                              (println e))))

                         (when (contains? local-response-verbs verb)
                           (try
                            (response-handlers/local-response-handler
                             aether history root-crsr v)
                            (catch js/Object e
                              (println e))))

                         (when (contains? remote-request-verbs verb)
                           (try
                            (->> (client-msg/local->remote history root-crsr v)
                                 ;; NOTE: sent with special verb that
                                 ;; gets picked up by http spout
                                 ;;
                                 ;; spout can feed response back into
                                 ;; aether so it'll be picked up by
                                 ;; local-response-handler
                                 (aether/send! aether)) 
                            (catch js/Object e
                              (println e))))
                         )

                       (recur (async/alts! [listener kill-ch]))))))
      (assoc this :kill-channel kill-ch)))

  (stop [this]
    (async/put! kill-channel :kill)
    (assoc this :kill-channel nil)))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->Dispatcher {:config config})))
