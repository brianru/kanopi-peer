(ns kanopi.controller.handlers.response
  (:require [om.core :as om]

            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]

            [kanopi.controller.handlers :as handlers :refer (local-event-handler)]

            [kanopi.aether.core :as aether]
            [kanopi.controller.history :as history]
            [kanopi.model.message :as msg]
            
            [kanopi.util.core :as util]))


(defmethod local-event-handler :datum.fact.update/success
  [aether history app-state msg]
  ;; TODO: this.
  )

(defmethod local-event-handler :datum.fact.update/failure
  [aether history app-state msg]
  ;; TODO this;
  )

;; FIXME: what about cache?
(defmethod local-event-handler :datum.label.update/success
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [datum-id (get-in msg [:noun :datum :db/id])]
                    (-> app-state
                        (assoc :datum (get-in msg [:noun]))
                        (assoc-in [:cache datum-id] (get-in msg [:noun :datum])))))))

(defmethod local-event-handler :datum.label.update/failure
  [aether history app-state msg]
  (om/transact! app-state :error-messages #(conj % msg)))

(defmethod local-event-handler :spa.navigate.search/success
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (merge app-state msg))))

(defmethod local-event-handler :spa.navigate.search/failure
  [aether history app-state msg]
  (om/transact! app-state :error-messages #(conj % msg)))

(defmethod local-event-handler :datum.create/success
  [aether history app-state msg]
  (let [dtm (get-in msg [:noun :datum])]
    (om/transact! app-state
                  (fn [app-state]
                   (-> app-state
                              (assoc :datum (get msg :noun))
                              (assoc-in [:cache (get dtm :db/id)] dtm))))
    (history/navigate-to! history [:datum :id (get dtm :db/id)])))

(defmethod local-event-handler :datum.create/failure
  [aether history app-state msg]
  (om/transact! app-state :error-messages #(conj % msg)))

(defmethod local-event-handler :datum.get/success
  [aether history app-state msg]
  (om/transact! app-state
                (fn [app-state]
                  (let [datum-id (get-in msg [:noun :datum :db/id])]
                    (-> app-state
                        (assoc :datum (get-in msg [:noun]))
                        (assoc-in [:cache datum-id] (get-in msg [:noun :datum])))))))

(defmethod local-event-handler :datum.get/failure
  [aether history app-state msg]
  (om/transact! app-state :error-messages #(conj % msg)))

(defmethod local-event-handler :spa.register/success
  [aether history app-state {:keys [noun] :as msg}]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user noun
                           :mode :spa.authenticated/online
                           :intent {:id :spa.authenticated/navigate}
                           :datum {:context-datums []
                                   :datum {}
                                   :similar-datums []}
                           :cache {}
                           :error-messages [])))
    (history/navigate-to! history :home)
    (->> (msg/initialize-client-state noun)
         (aether/send! aether))))

(defmethod local-event-handler :spa.register/failure
  [aether history app-state msg]
  (om/transact! app-state :error-messages #(conj % msg)))

;; TODO: this must get a lot more data. we must re-initialize
;; app-state with this users' data.
(defmethod local-event-handler :spa.login/success
  [aether history app-state {:keys [noun] :as msg}]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user noun
                           :mode :spa.authenticated/online
                           :intent {:id :spa.authenticated/navigate}
                           :datum {:context-datums []
                                   :datum {}
                                   :similar-datums []}
                           :cache {}
                           :error-messages [])))
    (history/navigate-to! history :home)
    (->> (msg/initialize-client-state noun)
         (aether/send! aether))))

(defmethod local-event-handler :spa.login/failure
  [aether history app-state msg]
  (let []
    (om/transact! app-state :error-messages #(conj % msg))))

(defmethod local-event-handler :spa.logout/success
  [aether history app-state msg]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (assoc app-state
                           :user nil
                           :mode :spa.unauthenticated/online
                           :intent {:id :spa.unauthenticated/navigate}
                           :datum {:context-datums []
                                   :datum []
                                   :similar-datums []}
                           :error-messages []
                           :cache {})))
    (history/navigate-to! history :home)))

(defmethod local-event-handler :spa.logout/failure
  [aether history app-state msg]
  (let []
    (om/transact! app-state :error-messages #(conj % msg))))

(defmethod local-event-handler :spa.state.initialize/success
  [aether history app-state msg]
  (let []
    (om/transact! app-state
                  (fn [app-state]
                    (merge app-state (get msg :noun))))))

(defmethod local-event-handler :spa.state.initialize/failure
  [aether history app-state msg]
  (om/transact! app-state :error-messages #(conj % msg)))


