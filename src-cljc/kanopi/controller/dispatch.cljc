(ns kanopi.controller.dispatch
  "Route messages traveling in the aether to local event handlers
  and/or spouts for external processing."
  #?(:cljs (:require-macros [cljs.core.async.macros :refer (go)])) 
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer (go)]
            [taoensso.timbre :as timbre :refer (log trace debug info warn error fatal report)]
            [kanopi.aether.core :as aether]
            [kanopi.controller.handlers.request :as request-handlers]
            [kanopi.controller.handlers.response :as response-handlers]
            [kanopi.model.message :as msg]
            ))

(defn- same-verbs-in-each-mode [modal-verbs]
  (let []
    ; FIXME: actually, :user/change-passsword should not be in every
    ; mode. think about this more carefully.
    ; also, login, logout and register:
    ; login/register are exclusive of logout. duh.
    true))

(defn- every-verb-has-matching-success-and-failure-verbs [modal-verbs]
  (let []
    true))

(defn mode-verbs []
  {:post [(same-verbs-in-each-mode %)
          (every-verb-has-matching-success-and-failure-verbs %)]}
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
             #{:spa/login :spa/register}
             :response
             #{}}}

   :spa.authenticated/online
   {:local  {:request
             #{:spa/navigate
               :spa/switch-team
               }
             :response
             #{:spa.logout/success   :spa.logout/failure

               :user.change-password/success    :user.change-password/failure

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
               :spa/logout
               :user/change-password

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
  [mode verbs history app-state {:keys [noun verb context] :as msg}]
  (let [local-request-verbs  (get-in verbs [mode :local :request])
        local-response-verbs (get-in verbs [mode :local :response])
        remote-request-verbs (get-in verbs [mode :remote :request])]
    (cond->> {:messages []}
      (contains? local-request-verbs verb)
      (merge-with concat
                  (try
                   (request-handlers/local-request-handler app-state msg)
                   (catch #?(:cljs js/Object :clj Exception) e
                     (info e)
                     {:messages []})))
      (contains? local-response-verbs verb)
      (merge-with concat
                  (try
                   (response-handlers/local-response-handler history app-state msg)
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
                  {:messages []}))))

(defprotocol IDispatcher
  "Dynamic message-oriented api."
  (transmit! [this msg]))

; NOTE: this still needs access to an HTTPSpout to work. Right now
; that's only done via an aether listen* call, so that'll have to be
; refactored.
; FIXME: actually, I broke this by using handle-message for
; everything. LOVELY!!!
(defrecord FunctionDispatcher [config verbs history app-state]
  IDispatcher
  (transmit! [this {:keys [noun verb context] :as msg}]
    ; NOTE: this decision of whether to hook into Om should not be
    ; made here. The State component may have to be aware of this and
    ; offer a protocol fn to provide access to the deref-able thing,
    ; whether it be an atom or an Om cursor.
    (let [atm     (:app-state app-state)
          mode    (get @atm :mode)
          results (handle-message mode verbs history atm msg)]
      (some->> (:messages results)
               (not-empty)
               (run! (partial transmit! this)))))
  component/Lifecycle
  (start [this]
    (assoc this :verbs (mode-verbs)))
  (stop [this]
    (assoc this :verbs nil)))

(defrecord AetherDispatcher [config aether history app-state kill-channel]
  IDispatcher
  (transmit! [this {:keys [noun verb context] :as msg}]
    (let [root-crsr (:app-state app-state)
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

(defn new-fn-dispatcher
  ([config]
   (map->FunctionDispatcher {:config config})))

(defn new-dispatcher
  ([] (new-dispatcher {}))
  ([config]
   (map->AetherDispatcher {:config config})))
