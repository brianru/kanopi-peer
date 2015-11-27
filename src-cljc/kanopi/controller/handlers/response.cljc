(ns kanopi.controller.handlers.response
  (:require #?(:cljs [om.core :as om]) 
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]

            [kanopi.aether.core :as aether]
            [kanopi.controller.history :as history]
            ; #?(:cljs [kanopi.controller.history.html5  :as history]
            ;    :clj  [kanopi.controller.history.memory :as history]) 
            [kanopi.model.message :as msg]
            
            [kanopi.util.core :as util]))

#?(:cljs
   (defn transact!
     ([crsr f]
      (om/transact! crsr f))
     ([crsr korks f]
      (om/transact! crsr korks f)))
   :clj
   (defn transact!
     ([atm f]
      (swap! atm f))
     ([atm korks f]
      (let [update-fn (if (coll? korks) update-in update)]
        (swap! atm #(update-fn % korks f))))))

; (defn transact!
;   ([crsr f]
;    (om/transact! crsr f))
;   ([crsr korks f]
;    (om/transact! crsr korks f)))

(defn update!
  ([crsr v]
   (transact! crsr (constantly v)))
  ([crsr korks v]
   (transact! crsr korks (constantly v))))

(defmulti local-response-handler
  (fn [_ _ _ msg]
    (info msg)
    (get msg :verb)))

(defn- record-error-message
  [app-state msg]
  (transact! app-state :error-messages #(conj % msg)))

(defmethod local-response-handler :spa.navigate/success
  [aether history app-state msg]
  (update! app-state :page (get msg :noun)))

(defn- incorporate-user-datum!
  [app-state user-datum]
  (transact! app-state
             (fn [app-state]
               (let [datum-id (get-in user-datum [:datum :db/id])]
                 (-> app-state
                     (assoc :datum user-datum)
                     (assoc-in [:cache datum-id] (get user-datum :datum)))))))

(defn- incorporate-updated-datum!
  [app-state {:keys [datum new-entities]}]
  (transact! app-state
             (fn [app-state]
               (println "incorporate updated datum" (get datum :datum/fact))
               (println new-entities)
               (let [datum-id (get datum :db/id)
                     cache-delta (->> (conj new-entities datum)
                                      (map (comp vec (juxt :db/id identity)))
                                      (into {}))
                     app-state' (-> app-state
                                    (assoc-in [:datum :datum] datum)
                                    (update :cache #(merge % cache-delta)))]
                 app-state'))))

(defmethod local-response-handler :datum.fact.add/success
  [aether history app-state msg]
  (incorporate-updated-datum! app-state (get msg :noun)))

(defmethod local-response-handler :datum.fact.add/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :datum.fact.update/success
  [aether history app-state msg]
  (incorporate-updated-datum! app-state (get msg :noun)))

(defmethod local-response-handler :datum.fact.update/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :datum.label.update/success
  [aether history app-state msg]
  (incorporate-updated-datum! app-state {:datum (get msg :noun)
                                         :new-entities []}))

(defmethod local-response-handler :datum.label.update/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :datum.create/success
  [aether history app-state msg]
  (let [dtm (get-in msg [:noun :datum])]
    (incorporate-user-datum! app-state (get msg :noun))
    (history/navigate-to! history [:datum :id (get dtm :db/id)])))

(defmethod local-response-handler :datum.create/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :datum.get/success
  [aether history app-state msg]
  (incorporate-user-datum! app-state (get msg :noun)))

(defmethod local-response-handler :datum.get/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :spa.navigate.search/success
  [aether history app-state msg]
  (let [{:keys [query-string results]} (get msg :noun)]
    (transact! app-state :search-results
               (fn [search-results]
                 (assoc search-results query-string results)))))

(defmethod local-response-handler :spa.navigate.search/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :spa.register/success
  [aether history app-state {:keys [noun] :as msg}]
  (let []
    (transact! app-state
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

(defmethod local-response-handler :spa.register/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :spa.login/success
  [aether history app-state {:keys [noun] :as msg}]
  (let []
    (transact! app-state
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

(defmethod local-response-handler :spa.login/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :spa.logout/success
  [aether history app-state msg]
  (let []
    (transact! app-state
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

(defmethod local-response-handler :spa.logout/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :spa.state.initialize/success
  [aether history app-state msg]
  (let []
    (transact! app-state
               (fn [app-state]
                 (merge app-state (get msg :noun))))))

(defmethod local-response-handler :spa.state.initialize/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

(defmethod local-response-handler :spa.switch-team/success
  [aether history app-state msg]
  (let [user' (get msg :noun)]
    (transact! app-state (fn [app-state]
                           (assoc app-state :user (get msg :noun))))
    ;; NOTE: user changed, therefore creds changed, therefore must
    ;; reinitialize => could have a current-datum which is not
    ;; accessible from the new creds
    (->> (msg/initialize-client-state user')
         (aether/send! aether))
    (history/navigate-to! history :home)))

(defn incorporate-updated-literal! [app-state literal]
  (println "incorporate-literal:" literal)
  (transact! app-state
             (fn [app-state]
               (let [literal-id (get literal :db/id)]
                 (-> app-state
                     (assoc-in [:literal :literal] literal)
                     (assoc-in [:cache literal-id] literal))))))

(defn incorporate-user-literal! [app-state user-literal]
  (transact! app-state
             (fn [app-state]
               (let [literal-id (get-in user-literal [:literal :db/id])]
                 (-> app-state
                     (assoc :literal user-literal)
                     (assoc-in [:cache literal-id] (get user-literal :literal)))
                 ))))

(defmethod local-response-handler :literal.get/success
  [aether history app-state msg]
  (let [literal (get-in msg [:noun])]
    (incorporate-user-literal! app-state literal)))
(defmethod local-response-handler :literal.get/failure
  [aether history app-state msg]
  (record-error-message app-state msg))

; FIXME: what if update converts literal to datum?
(defmethod local-response-handler :literal.update/success
  [aether history app-state msg]
  (let [literal (get-in msg [:noun])]
    (incorporate-updated-literal! app-state literal)))
(defmethod local-response-handler :literal.update/failure
  [aether history app-state msg]
  (record-error-message app-state msg))
