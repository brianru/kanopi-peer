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
            [hiccup.page :refer (html5 include-js include-css)]))

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
  :allowed-methods [:options :post]
  :available-media-types ["application/json"]
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (-> (friend/current-authentication (get-in ctx [:request]))
                   (rep/as-response ctx)
                   (rep/ring-response))))

(defresource ajax-logout-resource
  :allowed-methods [:options :post]
  :available-media-types ["application/json"]
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (-> (rep/ring-response {:logout-success true} ctx)
                   (friend/logout*))))

(defresource registration-resource
  :allowed-methods [:options :post]
  :available-media-types ["application/json"]
  :post! register!
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (if-let [user (get ctx ::identity)]
                 (-> (rep/as-response user (assoc ctx :status 500))
                     (friend/merge-authentication user)
                     (rep/ring-response))
                 (rep/ring-response (get ctx ::error) {:status 500}))))
