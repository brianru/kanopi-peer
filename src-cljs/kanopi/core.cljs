(ns kanopi.core
  (:require [quile.component :as component]
            [kanopi.system :as sys]
            [om.core :as om :include-macros true]
            [secretary.core :as secretary :include-macros true]
            [sablono.core :refer-macros [html] :include-macros true]
            [kanopi.view.fact :as fact]
            ))

(def app-container (. js/document (getElementById "app-container")))

(def app-state
  (atom {:tempo {:pulse nil}
         :user  {
                 :actions []
                 }
         :thunk {}
        }))

(defn root
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "app-root")

    om/IRender
    (render [_]
      (html
       [:div
        [:h1 "Hi!"]]
       ))))

(defn mount-root! []
  (om/root root app-state
           {:target app-container
            :shared {}}))

(mount-root!)

(def dev-config
  {})

(defonce system
  (component/start (sys/new-system dev-config)))

(defn init []
  (alter-var-root #'system (constantly (sys/new-system dev-config))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn go [] (init) (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
