(ns kanopi.devcards
  (:require [sablono.core :as sab])
  (:require-macros [devcards.core :refer (defcard)]))

(defcard my-first-card
  (sab/html [:h1 "Devcards is freaking awesome!"]))
