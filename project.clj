(defproject nebula "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [com.taoensso/timbre "3.4.0"]
                 [leiningen "2.5.1"]
                 [environ "1.0.0"]

                 ;; Database
                 [com.datomic/datomic-free "0.9.5153" :exclusions [joda-time]]

                 ;; Web
                 [org.immutant/web "2.0.0-beta2"]
                 [compojure "1.3.3"]
                 [liberator "0.12.2"]
                 [com.cemerick/friend "0.2.1"]
                 [ring/ring-defaults "0.1.4"]
                 [hiccup "1.0.5"]

                 ;; Test
                 [org.clojure/test.check "0.7.0"]
                 ]
  :main nebular.main
  :plugins [[lein-marginalia "0.8.0"]]
  :profiles {:dev {:plugins [[lein-environ "1.0.0"]
                             [lein-ancient "0.6.6"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [ring/ring-devel "1.3.1"]]
                   :env {:dev true}
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}})
