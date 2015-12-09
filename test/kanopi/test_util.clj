(ns kanopi.test-util
  (:require [com.stuartsierra.component :as component]
            [clojure.data.codec.base64 :as base64]
            [kanopi.main :refer (default-config)]
            [kanopi.system.server :as server]
            [kanopi.system.client :as client]
            [kanopi.controller.authenticator :as authenticator]
            [kanopi.model.storage.datomic :as datomic]
            [kanopi.model.session :as session]
            [datomic.api :as d]
            [kanopi.util.core :as util]
            [ring.mock.request :as mock]))

(defn system-excl-web []
  (-> (server/new-system default-config)
      (dissoc :web-app :web-server)))

(defn system-excl-web-server []
  (-> (server/new-system default-config)
      (dissoc :web-server)))

(defn mk-basic-auth-header [{:keys [username password] :as creds}]
  (->> (str username ":" password)
       (.getBytes)
       (base64/encode)
       (map char)
       (apply str)
       (str "Basic ")))

(defn assoc-basic-auth [m creds]
  (assoc-in m [:headers "authorization"] (mk-basic-auth-header creds)))

(defn get-db
  ([system]
   (get-db system nil))
  ([system creds]
   (datomic/db (get-in system [:datomic-peer]) creds)))


(defn mock-request! [system method route params & opts]
  (let [handler (get-in system [:web-app :app-handler])
        opts    (apply hash-map opts)
        req     (cond-> (mock/request method route params)
                  (find opts :creds)
                  (assoc-basic-auth (get opts :creds))
                  (find opts :content-type)
                  (mock/content-type (get opts :content-type))
                  (find opts :accept)
                  (mock/header :accept (get opts :accept))
                  )]
    (-> req
        (handler)
        (update :body util/transit-read)
        )))

(defn mock-register [system creds]
  (mock-request! system :post "/register" creds
                 :accept "application/transit+json"))

(defn mock-login [system creds]
  (mock-request! system :post "/login" creds
                 :accept "application/transit+json"))

(defn api-req! [system creds msg]
  (mock-request! system :post "/api" (util/transit-write msg)
                 :creds creds
                 :content-type "application/transit+json"))

(defn initialized-client-system
  ([config]
   (let [server-system (component/start (system-excl-web))
         anon-session  (session/init-anonymous-session (:session-service server-system))
         client-system (-> (util/deep-merge
                             config {:app-state {:initial-value anon-session}})
                           (client/new-system)) 
         ]
     (component/stop server-system)
     client-system))

  ([config username password]
   (let [server-system (component/start (system-excl-web))
         creds (let [auth-svc (:authenticator server-system)]
                 (authenticator/register! auth-svc username password)
                 (authenticator/credentials auth-svc username)) 
         user-session (session/init-session (:session-service server-system) creds)
         client-system (-> (util/deep-merge
                             config {:app-state {:initial-value user-session}}))
         ]
     (component/stop server-system)
     client-system)))
