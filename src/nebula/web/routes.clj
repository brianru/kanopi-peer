(ns nebula.web.routes
  (:require [compojure.core  :refer (routes GET POST PUT ANY)]
            [compojure.route :as route]
            [nebula.web.resources.auth :as auth]
            [nebula.web.resources.api  :refer (api-resource)]
            [nebula.web.resources.spa  :refer (spa-resource)]
            ))

(defn app-routes []
  (->
   (routes
    ;; authentication
    (ANY "/register" [] auth/registration-resource)
    (GET "/login"    [] auth/login-resource)
    (GET "/logout"   [] auth/logout-resource)

    (GET "/" [] spa-resource)
    (ANY "/api/:entity-id" [] api-resource)

    (route/files "" {:root "target/public"})
    (route/not-found "<h1>Page not found</h1>")
    )))
