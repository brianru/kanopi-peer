(defproject kanopi "0.1.0-SNAPSHOT"
  :description "A tool for exploring, expressing and documenting your mind."
  :url "http://kanopi.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [com.taoensso/timbre "4.1.0"]
                 [environ "1.0.0"]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.cognitect/transit-clj "0.8.281"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 ;; resolves a dependency issue with figwheel and
                 ;; core.async
                 [org.clojure/core.memoize "0.5.6"]

                 ;; Client
                 [org.clojure/clojurescript "1.7.48"]
                 [quile/component-cljs "0.2.4"]
                 [org.omcljs/om "0.9.0"
                  :exclusions [cljsjs/react cljsjs/react-with-addons]]
                 [cljsjs/react-with-addons "0.13.3-0"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.3.14"]
                 [com.andrewmcveigh/cljs-time "0.3.11"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.cemerick/url "0.1.1"]
                 [sablono "0.3.6"
                  :exclusions [cljsjs/react cljsjs/react-with-addons]]

                 ;; Database
                 [com.datomic/datomic-free "0.9.5206"
                  :exclusions [joda-time]]

                 ;; Web
                 [org.immutant/web "2.0.2"]
                 [compojure "1.4.0"]
                 [liberator "0.13"]
                 [com.cemerick/friend "0.2.1"]
                 [ring/ring-defaults "0.1.5"]
                 [hiccup "1.0.5"]
                 [crypto-password "0.1.3"]
                 [cheshire "5.5.0"]
                 [clj-time "0.11.0"]

                 ;; Test
                 [org.clojure/test.check "0.7.0"]
                 ]
  :main kanopi.main

  :plugins [[lein-marginalia "0.8.0"]
            [lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.7"
             :exclusions [org.clojure/core.async]]]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/out"]

  :profiles {:dev {:plugins [[lein-environ "1.0.0"]
                             [lein-ancient "0.6.6"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [ring/ring-devel "1.4.0"]
                                  [ring/ring-mock "0.2.0"]
                                  [org.clojure/data.codec "0.1.0"]
                                  [org.clojure/data.csv "0.1.3"]]
                   :env {:dev true}
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}}

  :figwheel {:server-logfile "target/logs/figwheel.log" }

  :cljsbuild {:builds
              [{:id "dev"
                :figwheel {:on-jsload "kanopi.core/reload-om"}
                :source-paths ["src-cljs"]
                :compiler {:output-to "resources/public/js/main.js"
                           :output-dir "resources/public/js/out"
                           :asset-path "js/out"
                           :main kanopi.core
                           :optimizations :none
                           :pretty-print true
                           :source-map "resources/public/js/source_map.js"}}]
              })
