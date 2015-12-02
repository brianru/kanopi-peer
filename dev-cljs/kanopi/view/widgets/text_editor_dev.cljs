(ns kanopi.view.widgets.text-editor-dev
  (:require-macros [devcards.core :as dc :refer (defcard deftest)])
  (:require [sablono.core :as sab]
            [cljs.test :refer-macros (is)]
            [com.stuartsierra.component :as component]
            [kanopi.util-dev :as dev-util]
            [kanopi.view.widgets.text-editor :as text-editor]
            ))

(defcard rich-text
  (dc/om-root text-editor/rich-text {})
  {}
  {:inspect-data true, :history true})

(defcard generic-code
  (dc/om-root text-editor/code {})
  {}
  {:inspect-data true, :history true})

(defcard latex
  (dc/om-root text-editor/code {})
  {}
  {:inspect-data true, :history true})

(defcard clojure
  (dc/om-root text-editor/code {})
  {}
  {:inspect-data true, :history true})
