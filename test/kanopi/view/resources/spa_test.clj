(ns kanopi.view.resources.spa-test
  "TODO: moved from initializing client via cookies to doing so by
  embedding JSON in html"
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

#_(deftest anonymous-cookie
    (let [system  (component/start (test-util/system-excl-web-server))
          handler (get-in system [:web-app :app-handler])]
      (testing "foo"
        (let [req (mock/request :get "/")
              resp (handler req)
              cookie (get-kanopi-init-cookie resp)]
          (is (->>
               cookie
               ((juxt :user :page :datum :cache))
               (every? not-empty)))
          ))
      (component/stop system)))

#_(deftest authenticated-cookie
    (let [system  (component/start (test-util/system-excl-web-server))
          handler (get-in system [:web-app :app-handler])
          creds   {:username "mickeymouse" :password "minneymouse"}
          _       (test-util/mock-register system creds)]
      (testing "bar"
        (let [req    (-> (mock/request :get "/")
                         (test-util/assoc-basic-auth creds))
              resp   (handler req)
              cookie (get-kanopi-init-cookie resp)]
          (is (->>
               cookie
               ((juxt :user :page :datum :cache))
               (every? not-empty)))
          ))))
