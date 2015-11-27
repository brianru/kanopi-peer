(ns kanopi.controller.history)

(defprotocol INavigator
  "Page-based API for changing app state. This is natural to users --
  lots of apps have a finite set of pages or page types (aka screens).
  So we have to reify that as a real concept in the code. It is not an
  annoying thing we deal with in apache configs, it is a real part of
  the application.
  
  TODO: back/forward/peek/pop etc fns for using navigation history.
  "
  (navigate-to! [this path])
  (get-route-for [this path]))
