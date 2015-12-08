(ns kanopi.view.literal-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [cljs.test :refer-macros (is)]
            [com.stuartsierra.component :as component]
            [kanopi.util-dev :as dev-util]
            
            [kanopi.view.literal :as literal]))

; (defonce system
;   (component/start (dev-util/new-system)))

(defcard empty-literal
  (dc/om-root literal/container {;:shared (dev-util/shared-state system)
                                 })
  {}
  {:inspect-data true, :history true}
  )

(defcard math-literal
  (dc/om-root literal/container {;:shared (dev-util/shared-state system)
                                 :init-state {}
                                 :state {}})
  {:literal {:literal/math "f(x) = x^{\\pi}"}}
  {:inspect-data true, :history true}
  )

(defcard text-literal
  (dc/om-root literal/container {
                                 })
  {:literal {:literal/text "The circle of life!!! naahhhhh, nahh di nahhh"}})

(defcard integer-literal
  (dc/om-root literal/container {})
  {:literal {:literal/integer 3}})

(defcard decimal-literal
  (dc/om-root literal/container {})
  {:literal {:literal/decimal 2.0}})

(defcard uri-literal
  (dc/om-root literal/container {})
  {:literal {:literal/uri "https://www.google.com"}})
