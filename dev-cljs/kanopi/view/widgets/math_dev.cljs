(ns kanopi.view.widgets.math-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [cljs.test :refer-macros (is)]
            [com.stuartsierra.component :as component]
            [kanopi.util-dev :as dev-util]
            [kanopi.view.widgets.math :as math]
            ))

(defcard pi
  (dc/om-root math/katex {:init-state {:input-value "\\pi"}})
  {})
