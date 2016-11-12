(ns kanopi.closure
 (:require-macros [devcards.core :refer (defcard deftest)])
 (:require [cljs.test :refer-macros (is)]
           [goog.string.linkify :as linkify])
 (:import [goog.format EmailAddress]
          [goog Uri]))

(deftest format-email-addresses
  ""
  (is (= 0 1))
  (is (true? (EmailAddress/isValidAddress "b@kanopi.io")))
  (is (false? (EmailAddress/isValidAddress "b@kanopi"))))

(deftest format-uris
  "goog.Uri/parse is too generous.
  goog.string.linkify/findFirstUrl is better
  "
  (is (nil? (Uri/parse "apple")))
  (is (empty? (linkify/findFirstUrl "apple")))
  (is (empty? (linkify/findFirstUrl "kanopi.io")))
  (is (not-empty (linkify/findFirstUrl "www.kanopi.io"))))
