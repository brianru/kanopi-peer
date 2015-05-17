(ns nebula.web.resources.app
  (:require [com.stuartsierra.component :as component]
            [immutant.web.middleware :refer [wrap-session]]
            [liberator.dev :refer [wrap-trace]]
            [liberator.representation :as rep]
            [ring.middleware.defaults :as defaults]
            [nebula.util.core :as util]))

(defn wrap-ensure-session
  "Ensures that a session exists and has a key set. Also ensures that it's added
   to the response if missing."
  [h]
  (fn [req]
    (let [session (:session req)
          has-key? (contains? session :key)
          key (or (:key session) (str (util/random-uuid)))
          response (h (cond-> req
                              (not has-key?)
                              (assoc-in [:session :key] key)))]
      (cond

       (or has-key?
           (= key (get-in response [:session :key]))
           (and (contains? response :session) (nil? (:session response))))
       response

       (:session response)
       (assoc-in response [:session :key] key)

       :else
       (assoc response :session (assoc session :key key))))))

(defrecord WebApp
    [config datomic app-handler]
  component/Lifecycle
  (start [this]
    (if app-handler this
        (-> defaults/site-defaults
            (->> (defaults/wrap-defaults ((:handler config))))
            (cond-> (:dev config)
              (wrap-trace :header :ui))
            (wrap-ensure-session)
            (wrap-session {:timeout 0})
            (->> (assoc this :app-handler)))))
  (stop [this]
    (if-not app-handler this
      (assoc this :app-handler nil))))

(defn get-handler [web-app]
  (:app-handler web-app))

(defn new-web-app [config]
  (map->WebApp {:config config}))
