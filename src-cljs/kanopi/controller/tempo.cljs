(ns kanopi.controller.tempo
  "TODO: zelkova."
  (:require [om.core :as om]
            [sablono.core :refer-macros [html] :include-macros true]
            [shodan.console :refer-macros [log] :include-macros true]
            ))

(defn heart-rate-monitor
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "heart-rate-monitor")

    om/IWillMount
    (will-mount [_]
      ;; apply heart monitor
      ;; take stream of user actions - update pulse accordingly.
      )

    om/IRender
    (render [_]
      (html [:span]))))

(defn progress-monitor
  [props owner opts])

(defn goal-monitor
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "goal-monitor")

    om/IWillMount
    (will-mount [_]
      ;; apply goal monitor
      ;; take stream of user actions -- interpret user's current goal
      )

    om/IRender
    (render [_]
      (html [:span]))))

(defn mood-monitor
  [props owner opts]
  (reify
    om/IDisplayName
    (display-name [_]
      "mood-monitor")

    om/IWillMount
    (will-mount [_]
      ;; apply mood monitor
      ;; not sure how this works
      ;; but ideally i know whether my users are happy or sad or frustrated or focused
      ;;
      ;; I suppose this is a combination of the other monitors.
      ;; goal + progress + tempo => approx mood?
      )

    om/IRender
    (render [_]
      (html [:span]))))
