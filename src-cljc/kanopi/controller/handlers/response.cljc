(ns kanopi.controller.handlers.response
  "TODO: refactor to return a collection of messages when necessary.
  The dispatcher should handle message delivery."
  (:require #?(:cljs [om.core :as om]) 
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) (log trace debug info warn error fatal report)]

            [kanopi.controller.history :as history]
            [kanopi.model.message :as msg]
            
            [kanopi.util.core :as util]))

#?(:cljs
   (defn transact!
     ([crsr f]
      (om/transact! crsr f))
     ([crsr korks f]
      (println "cljs transact!" korks)
      (println @crsr)
      (println (type crsr))
      (om/transact! crsr korks f)))
   :clj
   (defn transact!
     ([atm f]
      (swap! atm f))
     ([atm korks f]
      (let [update-fn (if (coll? korks) update-in update)]
        (swap! atm #(update-fn % korks f))))))

(defn update!
  ([crsr v]
   (transact! crsr (fn [_] v)))
  ([crsr korks v]
   (println "update!" korks v)
   (transact! crsr korks (fn [_] v))))

(defmulti local-response-handler
  (fn [_ _ msg]
    (info msg)
    (get msg :verb)))

(defn- record-error-message
  [app-state msg]
  (transact! app-state :error-messages #(conj % msg)))

(defmethod local-response-handler :spa.navigate/success
  [history app-state msg]
  (println "navigate-success" msg)
  (update! app-state :page (get msg :noun))
  (hash-map :messages []))

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
  [history app-state msg]
  (incorporate-updated-datum! app-state (get msg :noun))
  (hash-map :messages []))

(defmethod local-response-handler :datum.fact.add/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :datum.fact.update/success
  [history app-state msg]
  (incorporate-updated-datum! app-state (get msg :noun))
  (hash-map :messages []))

(defmethod local-response-handler :datum.fact.update/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :datum.label.update/success
  [history app-state msg]
  (incorporate-updated-datum! app-state {:datum (get msg :noun)
                                         :new-entities []})
  (hash-map :messages []))

(defmethod local-response-handler :datum.label.update/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :datum.create/success
  [history app-state msg]
  (let [dtm (get-in msg [:noun :datum])]
    (incorporate-user-datum! app-state (get msg :noun))
    (history/navigate-to! history [:datum :id (get dtm :db/id)]))
  (hash-map :messages []))

(defmethod local-response-handler :datum.create/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :datum.get/success
  [history app-state msg]
  (incorporate-user-datum! app-state (get msg :noun))
  (hash-map :messages []))

(defmethod local-response-handler :datum.get/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :spa.navigate.search/success
  [history app-state msg]
  (let [{:keys [query-string results]} (get msg :noun)]
    (transact! app-state :search-results
               (fn [search-results]
                 (assoc search-results query-string results))))
  (hash-map :messages []))

(defmethod local-response-handler :spa.navigate.search/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :spa.register/success
  [history app-state {:keys [noun] :as msg}]
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
    (hash-map :messages [(msg/initialize-client-state noun)])))

(defmethod local-response-handler :spa.register/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :spa.login/success
  [history app-state {:keys [noun] :as msg}]
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
    (hash-map :messages [(msg/initialize-client-state noun)])))

(defmethod local-response-handler :spa.login/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :spa.logout/success
  [history app-state msg]
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
    (history/navigate-to! history :home))
  (hash-map :messages []))

(defmethod local-response-handler :spa.logout/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :spa.state.initialize/success
  [history app-state msg]
  (let []
    (transact! app-state
               (fn [app-state]
                 (merge app-state (get msg :noun)))))
  (hash-map :messages []))

(defmethod local-response-handler :spa.state.initialize/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

(defmethod local-response-handler :spa.switch-team/success
  [history app-state msg]
  (let [user' (get msg :noun)]
    (transact! app-state (fn [app-state]
                           (assoc app-state :user (get msg :noun))))
    ;; NOTE: user changed, therefore creds changed, therefore must
    ;; reinitialize => could have a current-datum which is not
    ;; accessible from the new creds
    (history/navigate-to! history :home)
    (hash-map :messages [(msg/initialize-client-state user')])
    ))

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
  [history app-state msg]
  (let [literal (get-in msg [:noun])]
    (incorporate-user-literal! app-state literal))
  (hash-map :messages []))
(defmethod local-response-handler :literal.get/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))

; FIXME: what if update converts literal to datum?
(defmethod local-response-handler :literal.update/success
  [history app-state msg]
  (let [literal (get-in msg [:noun])]
    (incorporate-updated-literal! app-state literal))
  (hash-map :messages []))
(defmethod local-response-handler :literal.update/failure
  [history app-state msg]
  (record-error-message app-state msg)
  (hash-map :messages []))
