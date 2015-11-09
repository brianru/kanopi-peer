(ns kanopi.view.resources.spa-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer (pprint)]
            
            [com.stuartsierra.component :as component]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [schema.core :as s]

            [ring.mock.request :as mock]

            [kanopi.model.schema :as schema]
            
            [kanopi.util.core :as util]
            [kanopi.test-util :as test-util]
            ))

(defn get-kanopi-init-cookie [resp]
  (-> resp
      (get-in [:headers "Set-Cookie"])
      (first)
      (clojure.string/replace-first #"kanopi-init=" "")
      (url/query->map)
      (get "init")
      (json/parse-string true)
      ))

(deftest anonymous-cookie
  (let [system  (component/start (test-util/system-excl-web-server))
        handler (get-in system [:web-app :app-handler])]
    (testing "foo"
      (let [req (mock/request :get "/")
            resp (handler req)
            cookie (get-kanopi-init-cookie resp)]
        (is (not-empty (get cookie :user)))
        (is (not-empty (get cookie :datum)))
        (is (not-empty (get cookie :cache)))
        ))
    (component/stop system)))
