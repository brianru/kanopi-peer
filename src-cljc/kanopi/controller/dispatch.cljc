(ns kanopi.controller.dispatch
  "Route messages traveling in the aether to local event handlers
  and/or spouts for external processing."
  #?(:cljs (:require-macros [cljs.core.async.macros :refer (go)])) 
  (:require [com.stuartsierra.component :as component]
            #?(:clj  [clojure.core.async :as async :refer (go)]
               :cljs [cljs.core.async :as async])
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]
            [kanopi.aether.core :as aether]
            [kanopi.controller.handlers.request :as request-handlers]
            [kanopi.controller.handlers.response :as response-handlers]
            [kanopi.model.message :as msg]
            [kanopi.model.message.client :as client-msg]
            #?(:cljs [om.core :as om])
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

(defn- handle-message
  [mode verbs history atm {:keys [noun verb context] :as msg}]
  (let [local-request-verbs  (get-in verbs [mode :local :request])
        local-response-verbs (get-in verbs [mode :local :response])
        remote-request-verbs (get-in verbs [mode :remote :request])
        ]
    (cond->> {:messages []}
      (contains? local-request-verbs verb)
      (merge-with concat
                  (try
                   (request-handlers/local-request-handler atm msg)
                   (catch #?(:cljs js/Object :clj Exception) e
                     (info e)
                     {:messages []})))
      (contains? local-response-verbs verb)
      (merge-with concat
                  (try
                   (response-handlers/local-response-handler history atm msg)
                   (catch #?(:cljs js/Object :clj Exception) e
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
                  {:messages [(client-msg/local->remote history atm msg)]}))))

(defprotocol IDispatcher
  "Dynamic message-oriented api."
  (transmit! [this msg]))

(defrecord FunctionDispatcher [config history app-state]
  IDispatcher
  (transmit! [this {:keys [noun verb context] :as msg}]
    ; NOTE: this decision of whether to hook into Om should not be
    ; made here. The State component may have to be aware of this and
    ; offer a protocol fn to provide access to the deref-able thing,
    ; whether it be an atom or an Om cursor.
    (let [atm     (#?(:cljs om/root-cursor
                      :clj  identity)     (:app-state app-state))
          mode    (get @atm :mode)
          verbs   (mode-verbs)
          results (handle-message mode verbs history atm msg)
          ]
      (when-let [msgs (not-empty (:messages results))]
        (dorun
         (map (partial transmit! this) msgs))))))

(defrecord AetherDispatcher [config aether history app-state kill-channel]
  IDispatcher
  (transmit! [this {:keys [noun verb context] :as msg}]
    (let [root-crsr (#?(:cljs om/root-cursor
                              :clj  identity)     (:app-state app-state))
          mode      (get @root-crsr :mode)
          verbs     (mode-verbs)
          results   (handle-message mode verbs history root-crsr msg)
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
      (go (loop [[v ch] (repeat nil)]
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
