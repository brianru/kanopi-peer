(ns nebula.web.routes
  (:require [compojure.core  :refer (routes GET POST PUT ANY)]
            [compojure.route :as route]
            [nebula.web.resources.auth :refer (login-resource logout-resource)]
            [nebula.web.resources.api  :refer (api-resource)]
            [nebula.web.resources.spa  :refer (spa-resource)]
            ))

(defn app-routes []
  (->
   (routes
    ;; authentication
    (GET "/login"  [] login-resource)
    (GET "/logout" [] logout-resource)

    (GET "/" [] spa-resource)
    (ANY "/api/:entity-id" [] api-resource)

    (route/files "" {:root "target/public"})
    (route/not-found "<h1>Page not found</h1>")
    )))
