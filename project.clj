(defproject io.kanopi/kanopi "0.1.0-SNAPSHOT"
  :description "A tool for exploring, expressing and documenting your thoughts."
  :url "http://kanopi.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.10.0-RC1"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [org.clojure/core.async "0.4.474"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 ;; resolves a dependency issue with figwheel and
                 ;; core.async
                 [org.clojure/core.memoize "0.7.1"]

                 [com.stuartsierra/component "0.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [environ "1.1.0"]

                 ;; Fuzzy string matching
                 [clj-fuzzy "0.4.1"]

                 ;; Database
                 ; NOTE: add AWS DDB sdk when upgrading datomic
                 ; http://docs.datomic.com/storage.html#provisioning-dynamo
                 [com.datomic/datomic-pro "0.9.5783"
                  :exclusions [joda-time]]
                 [alandipert/enduro "1.2.0"]

                 [cljs-ajax "0.7.4"]

                 ;; Web App
                 [org.immutant/web "2.1.10"]
                 [compojure "1.6.1"]
                 [liberator "0.15.2"]
                 ;; NOTE: this is installed in my local maven
                 ;; manually.
                 [com.cemerick/friend "0.2.3"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-middleware-format "0.7.2"]
                 [hiccup "2.0.0-alpha1"]
                 [crypto-password "0.2.0"]
                 [cheshire "5.8.1"] ;;; JSON
                 [clj-time "0.14.5"]
                 [garden "1.3.6"]

                 [ring/ring-devel "1.7.0"]

                 ;; Specification and Testing
                 [prismatic/schema "1.1.9"]
                 [org.clojure/test.check "0.10.0-alpha3"]]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:env/kanopi_datomic_username]
                                   :password [:env/kanopi_datomic_password]}}
  :source-paths ["src" "src-cljc"]
  :test-paths ["src" "src-cljc" "test"]
  :main kanopi.main

  :plugins [[lein-environ "1.0.1"]]

  :aliases {"build!" ["do" "clean" ["with-profile" "prod" "uberjar"]]}

  :profiles {:prod
             {:env {:dev "false"}}

             :uberjar
             {:uberjar-name "kanopi-uberjar.jar"
              :aot :all
              ; this is done as part of build! alias. otherwise
              ; uberjar calls 'clean' which wipes out the compiled js
              ; resources
              :auto-clean false}

             :dev
             {:plugins [[lein-ancient "0.6.15"]]
              :dependencies [[org.clojure/tools.nrepl "0.2.13"]
                             [org.clojure/tools.namespace "0.3.0-alpha4"]
                             [ring/ring-devel "1.7.0"]
                             [ring/ring-mock "0.3.2"]
                             [org.clojure/data.codec "0.1.1"]
                             [org.clojure/data.csv "0.1.4"]]
              :env {:dev "true"}
              :source-paths ["dev"]
              :repl-options {:init-ns user}}})
