(ns kanopi.util.async
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [cljs.core.async :as async]))

(defn debounce
  ([source msecs]
    (debounce (async/chan) source msecs))
  ([c source msecs]
    (asyncm/go
      (loop [state ::init cs [source]]
        (let [[_ threshold] cs]
          (let [[v sc] (async/alts! cs)]
            (condp = sc
              source (condp = state
                       ::init
                         (do (async/>! c v)
                           (recur ::debouncing
                             (conj cs (async/timeout msecs))))
                       ::debouncing
                         (recur state
                           (conj (pop cs) (async/timeout msecs))))
              threshold (recur ::init (pop cs)))))))
    c))
