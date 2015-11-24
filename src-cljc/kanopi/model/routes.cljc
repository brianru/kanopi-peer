(ns kanopi.model.routes
  (:require [bidi.bidi :as bidi]))

; IDEA: first part indicates team
(def client-routes
  ["/" {
        ;; SPA
        ""         :home
        "enter"    :enter
        "login"    :login
        "logout"   :logout
        "register" :register
        "teams"    :teams
        "settings" :settings
        "datum/"   {[:id ""] :datum}
        "literal/" {[:id ""] :literal}

        ;; Server
        "api"      :api}])

(def server-routes
  [])
