(defproject kanopi "0.1.0-SNAPSHOT"
  :description "A tool for exploring, expressing and documenting your mind."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [com.taoensso/timbre "3.4.0"]
                 [environ "1.0.0"]

                 ;; Client
                 [org.clojure/clojurescript "0.0-3119"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"
                  :exclusions [cljsjs/react cljsjs/react-with-addons]]
                 [cljsjs/react-with-addons "0.12.2-8"]
                 [secretary "1.2.3"]
                 [shodan "0.4.1"]
                 [cljs-ajax "0.3.10"]
                 [sablono "0.3.4"
                  :exclusions [cljsjs/react cljsjs/react-with-addons]]

                 ;; Database
                 [com.datomic/datomic-free "0.9.5153"
                  :exclusions [joda-time]]

                 ;; Web
                 [org.immutant/web "2.0.0"]
                 [compojure "1.3.3"]
                 [liberator "0.12.2"]
                 [com.cemerick/friend "0.2.1"]
                 [ring/ring-defaults "0.1.4"]
                 [hiccup "1.0.5"]
                 [crypto-password "0.1.3"]

                 ;; Test
                 [org.clojure/test.check "0.7.0"]
                 ]
  :main kanopi.main
  :plugins [[lein-marginalia "0.8.0"]
            [lein-cljsbuild "1.0.6"]]
  :profiles {:dev {:plugins [[lein-environ "1.0.0"]
                             [lein-ancient "0.6.6"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [ring/ring-devel "1.3.1"]]
                   :env {:dev true}
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}}

  :cljsbuild {:builds
              {:dev {:source-paths ["src-cljs/kanopi/"]
                     :compiler {:output-to "target/public/js/main.js"
                                :output-dir "target/public/js/out"
                                :optimizations :simple
                                :pretty-print true
                                :source-map "target/public/js/source_map.js"}}
               }})
