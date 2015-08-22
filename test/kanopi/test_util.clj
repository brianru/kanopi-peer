(ns kanopi.test-util
  (:require [com.stuartsierra.component :as component]
            [clojure.data.codec.base64 :as base64]
            [kanopi.main :refer (default-config)]
            [kanopi.system :refer (new-system)]))

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
