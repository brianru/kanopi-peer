(ns nebula.data
  "Generic API to a Database component.

  TODO: query protocol
  "
  (:require [datomic.api :as d]))

(defprotocol IQuery
  "docstring"
  )
