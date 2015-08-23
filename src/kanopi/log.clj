(ns kanopi.log
  "Log all messages so entire system can be replayed.
  
  This manifests itself as a system component which takes a message
  and asynchronously sends it to a log store -- maybe just S3 with
  json. Ideally this component can also help access such logs."
  )

;; TODO: http resources must log requests here
