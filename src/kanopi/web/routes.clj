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
    (GET "/register" [] spa-resource)
    (POST "/register" [] auth/registration-resource)

    (GET "/login" [] spa-resource)
    ;; friend intercepts POST then passes on to here
    (POST "/login" [] auth/ajax-login-resource)

    (GET "/logout" [] spa-resource)
    ;;(GET "/login"    [] auth/login-resource)
    ;;(GET "/logout"   [] auth/logout-resource)

    (ANY "/api/" [] api-resource)
    (GET "/thunk/:id" [] spa-resource)
    (GET "/settings" [] spa-resource)
    (GET "/" [] spa-resource)

    (route/files "" {:root "target/public"})
    (route/not-found "<h1>Page not found</h1>")
    )))
