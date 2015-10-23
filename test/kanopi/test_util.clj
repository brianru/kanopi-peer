(ns kanopi.test-util
  (:require [com.stuartsierra.component :as component]
            [clojure.data.codec.base64 :as base64]
            [kanopi.main :refer (default-config)]
            [kanopi.system :refer (new-system)]
            [datomic.api :as d]
            [kanopi.util.core :as util]
            [ring.mock.request :as mock]))

(defn system-excl-web []
  (-> (new-system default-config)
      (dissoc :web-app :web-server)))

(defn system-excl-web-server []
  (-> (new-system default-config)
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

(defn get-db [system]
  (d/db (get-in system [:datomic-peer :connection])))


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
