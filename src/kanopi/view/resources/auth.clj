(ns kanopi.view.resources.auth
  "BEWARE: this used to be an html/redirect-based auth flow. I then
  refactored it into a pure transit+json flow but I left the bones of
  the old flow because at some point I'll want both to work."
  (:require [liberator.core :refer [defresource]]
            [cemerick.friend :as friend]
            [kanopi.controller.authenticator :as auth]
            [kanopi.util.core :as util]
            [liberator.representation :as rep]
            [ring.util.response :as r]
            [cheshire.core :as json]
            [hiccup.page :refer (html5 include-js include-css)]
            [kanopi.model.data :as data]))


;; Override liberator's json generation, which uses clojure.data.json, to use
;; Cheshire. Fills one specific gap in clojure.data.json which is writing
;; java.util.Date instances. There are probably other justifications as well.
(defmethod liberator.representation/render-map-generic "applicaion/json"
  [data context]
  (json/generate-string data))

(defmethod liberator.representation/render-seq-generic "applicaion/json"
  [data context]
  (json/generate-string data))


(defn register!
  [ctx]
  (try
    (let [params                      (get-in ctx [:request :params])
          body                        (json/parse-string (slurp (get-in ctx [:request :body])) true)
          {:keys [username password]} (merge params body)
          authenticator               (util/get-authenticator ctx)
          user-ent-id                 (auth/register! authenticator username password)]
      {::result   {:db/id user-ent-id}
       ;; NOTE: could this actually created an authenticated response?
       ;; I don't think that's happening b/c registration does not flow
       ;; through friend.
       ;; FIXME: refactor to 'creds->friend-identity'
       ::identity (-> (auth/credentials authenticator username)
                      (dissoc :password)
                      ;; to make this match the friend
                      ;; current-authentication map
                      ((fn [x] (-> x
                                   (dissoc :team)
                                   (assoc :identity (:username x))))))})
    (catch Throwable t
      {::error {:error true :message (.getMessage t)}})))

(defresource ajax-login-resource
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (let [creds (friend/current-authentication (get-in ctx [:request]))
                     payload {:user creds
                              :transactions (data/recent-activity (util/get-data-service ctx) creds)
                              ;; :recent-datums (data/recent-datums (util/get-data-service ctx) creds )
                              }]
                 (rep/ring-response payload ctx))))

(defresource ajax-logout-resource
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (-> (rep/ring-response {:logout-success true} ctx)
                   (friend/logout*))))

(defresource registration-resource
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :post! register!
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (if-let [user (get ctx ::identity)]
                 (rep/ring-response
                  {:user user
                   :transactions (data/recent-activity (util/get-data-service ctx) user)}
                  (friend/merge-authentication ctx user))
                 (rep/ring-response (get ctx ::error) {:status 500}))))
