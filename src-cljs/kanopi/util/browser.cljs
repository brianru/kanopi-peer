(ns kanopi.util.browser
  (:require [om.core :as om]))

(defn route-for [owner kw]
  ((om/get-shared owner [:history :route-for]) kw))
