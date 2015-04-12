(ns nebula.web.routes
  (:require [compojure.core :refer [routes GET POST PUT ANY]]
            [compojure.route :as route]
            [nebula.web.auth :refer [authentication-resource]]
            [nebula.web.api :refer [api-resource]]
            [nebula.web.spa :refer [spa-resource]]
            ))

(defn app-routes []
  (->
   (routes
    ;; authentication
    (ANY "/authentication" [] authentication-resource)

    ;; base
    (GET "/" [] spa-resource)

    ;; api
    (ANY "/api/:entity-id" [] api-resource)


    (route/not-found "<h1>Page not found</h1>")
    )))
