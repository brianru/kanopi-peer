(ns kanopi.web.routes
  (:require [compojure.core  :refer (routes GET POST PUT ANY)]
            [compojure.route :as route]
            [kanopi.web.resources.auth :as auth]
            [kanopi.web.resources.api  :as api :refer (api-resource)]
            [kanopi.web.resources.spa  :as spa :refer (spa-resource)]
            ))

(defn app-routes []
  (->
   (routes

    ;; authentication
    (GET  "/register" [] spa-resource)
    (POST "/register" [] auth/registration-resource)

    (GET  "/login"    [] spa-resource)
    ;; friend intercepts POST then passes on to here
    (POST "/login"    [] auth/ajax-login-resource)

    (GET  "/logout"   [] spa-resource)
    (POST "/logout"   [] auth/ajax-logout-resource)

    ;; api
    (ANY "/api"       [] api-resource)

    ;; spa
    (GET "/"          [] spa-resource)
    (GET "/settings"  [] spa-resource)
    (GET "/datum/:id" [] spa-resource)

    ;; static assets
    (route/files "" {:root "target/public"})

    ;; TODO: better not-found page
    (route/not-found "<h1>Page not found</h1>")
    )))
