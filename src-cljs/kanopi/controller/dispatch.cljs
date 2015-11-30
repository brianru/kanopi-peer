(ns kanopi.controller.dispatch
  "Route messages traveling in the aether to local event handlers
  and/or spouts for external processing."
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async :as async]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.aether.core :as aether]
            [kanopi.controller.handlers.request :as request-handlers]
            [kanopi.controller.handlers.response :as response-handlers]
            [kanopi.model.message :as msg]
            [kanopi.model.message.client :as client-msg]
            [om.core :as om]
            ))

(defn- same-verbs-in-each-mode [modal-verbs]
  (let []
    true))

(defn- every-verb-has-matching-success-and-failure-verbs [modal-verbs]
  (let []
    true))

(defn mode-verbs []
  {:post [(same-verbs-in-each-mode %)
          (every-verb-has-matching-success-and-failure-verbs %)
          ]}
  {:spa.unauthenticated/online
   {:local  {:request
             #{:spa/navigate
               :spa.navigate/search
               :datum/create
               :datum/get
               :datum.fact/add
               :datum.fact/update
               :datum.label/update

               :literal/get
               :literal/update
               }
             :response
             #{
               :spa.login/success    :spa.login/failure
               :spa.logout/success   :spa.logout/failure
               :spa.register/success :spa.register/failure

               :spa.navigate/success            :spa.navigate/failure
               :spa.navigate.search/success     :spa.navigate.search/failure

               :datum.create/success            :datum.create/failure
               :datum.get/success               :datum.get/failure
               :datum.fact.add/success          :datum.fact.add/failure
               :datum.fact.update/success       :datum.fact.update/failure
               :datum.label.update/success      :datum.label.update/failure

               :literal.get/success             :literal.get/failure
               :literal.update/success          :literal.update/failure
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


               :spa.navigate/success            :spa.navigate/failure
               :spa.navigate.search/success     :spa.navigate.search/failure

               :datum.create/success            :datum.create/failure
               :datum.get/success               :datum.get/failure
               :datum.fact.add/success          :datum.fact.add/failure
               :datum.fact.update/success       :datum.fact.update/failure
               :datum.label.update/success      :datum.label.update/failure

               :spa.state.initialize/success    :spa.state.initialize/failure

               :literal.get/success             :literal.get/failure
               :literal.update/success          :literal.update/failure
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

               :literal/get
               :literal/update

               :spa.state/initialize
               }
             :response
             #{}
             }}})

(defprotocol IDispatcher
  "Dynamic message-oriented api."
  (transmit! [this msg]))

(defrecord FunctionDispatcher [config history app-state]
  IDispatcher
  (transmit! [this {:keys [noun verb context] :as msg}]
    (let [atm   (get app-state :app-state)
          mode  (get @atm :mode)
          verbs (mode-verbs)
          local-request-verbs  (get-in verbs [mode :local :request])
          local-response-verbs (get-in verbs [mode :local :response])
          remote-request-verbs (get-in verbs [mode :remote :request])
          ]
      )))

(defrecord AetherDispatcher [config aether history app-state kill-channel]
  IDispatcher
  (transmit! [this {:keys [noun verb context] :as msg}]
    (let [root-crsr (om/root-cursor (:app-state app-state))
          mode      (get @root-crsr :mode)
          verbs     (mode-verbs)
          local-request-verbs  (get-in verbs [mode :local :request])
          local-response-verbs (get-in verbs [mode :local :response])
          remote-request-verbs (get-in verbs [mode :remote :request])

          results
          (cond->> {:messages []}
            (contains? local-request-verbs verb)
            (merge-with concat
                        (try
                         (request-handlers/local-request-handler
                          aether root-crsr msg)
                         (catch js/Object e
                           (info e)
                           {:messages []})))
            (contains? local-response-verbs verb)
            (merge-with concat
                        (try
                         (response-handlers/local-response-handler
                          aether history root-crsr msg)
                         (catch js/Object e
                           (info e)
                           {:messages []})))

            ;; NOTE: sent with special verb that
            ;; gets picked up by http spout
            ;;
            ;; spout can feed response back into
            ;; aether so it'll be picked up by
            ;; local-response-handler
            (contains? remote-request-verbs verb)
            (merge-with concat
                        {:messages [(client-msg/local->remote history root-crsr msg)]}))
          ]
      (aether/send-many! aether (remove nil? (get results :messages)))
      ; TODO: considerably more sophisticated
      ; exception handling. at least log them!
      ))
  component/Lifecycle
  (start [this]
    (info "start dispatcher" (get config :mode))
    (let [kill-ch  (async/chan 1)
          listener (aether/replicate! aether)]
      (asyncm/go (loop [[v ch] (repeat nil)]
                   (if (= ch kill-ch)
                     (do
                      (async/close! kill-ch))
                     (do
                      ;; first run v is nil
                      (when v
                        (transmit! this v))
                      (recur (async/alts! [listener kill-ch]))))))
      (assoc this :kill-channel kill-ch)))

  (stop [this]
    (async/put! kill-channel :kill)
    (assoc this :kill-channel nil)))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->AetherDispatcher {:config config})))
