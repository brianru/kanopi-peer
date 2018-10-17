(ns kanopi.view.routes
  ""
  (:require [compojure.core  :refer (routes GET POST PUT ANY)]
            [compojure.route :as route]
            [kanopi.view.resources.auth :as auth]
            [kanopi.view.resources.api  :as api :refer (api-resource)]
            [kanopi.view.resources.spa  :as spa :refer (spa-resource)]))

(defn app-routes []
  (->
   (routes

    ;; authentication
    (GET  "/enter" [] spa-resource)
    ;; (GET  "/register" [] spa-resource)
    (ANY "/register" [] auth/registration-resource)

    ;; (GET  "/login"    [] spa-resource)
    ;; friend intercepts POST then passes on to here
    (POST "/login"    [] auth/ajax-login-resource)

    ;; (GET  "/logout"   [] spa-resource)
    (POST "/logout"   [] auth/ajax-logout-resource)

    ;; api
    (ANY "/api"       [] api-resource)

    ;; spa
    (GET "/"            [] spa-resource)
    (GET "/teams"       [] spa-resource)
    (GET "/settings"    [] spa-resource)
    (GET "/datum/:id"   [] spa-resource)
    (GET "/literal/:id" [] spa-resource)

    ;; static assets
    (route/files "" {:root "target/public"})

    ;; TODO: better not-found page
    (route/not-found "<h1>Page not found</h1>")
    )))
