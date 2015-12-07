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

; NOTE: I don't know if this is correct. Experimenting with bidi.
; TODO: test.
(def server-routes
  ["/"
   {:get {(bidi/alts ["enter" "register" "login" "logout"
                      "" "teams" "settings"
                      {"datum/" [:id ""]} {"literal" [:id ""]}])
          :single-page-app}
    :post {"register" :registration
           "login"    :login
           "logout"   :logout
           }
    "api" :api
    "" (bidi/->Files {:dir "resources/public"})
    true (fn [req] {:status 200 :body "<h1>Page not found</h1>"})

    }])
