(ns kanopi.util.browser
  (:require [om.core :as om]))

(defn route-for [owner & args]
  (apply (om/get-shared owner [:history :route-for]) args))
