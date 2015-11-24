(defproject io.kanopi/kanopi "0.1.0-SNAPSHOT"
  :description "A tool for exploring, expressing and documenting your thoughts."
  :url "http://kanopi.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.3.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [environ "1.0.1"]
                 [com.cognitect/transit-clj "0.8.285"]

                 ;; Core libraries
                 [org.clojure/core.async "0.2.374"]
                 [com.cognitect/transit-cljs "0.8.232"]
                 ;; resolves a dependency issue with figwheel and
                 ;; core.async
                 [org.clojure/core.memoize "0.5.8"]

                 ;; Fuzzy string matching
                 [clj-fuzzy "0.3.1"]

                 ;; Client
                 [org.clojure/clojurescript "1.7.170"]
                 [org.omcljs/om "1.0.0-alpha14"
                  :exclusions [cljsjs/react cljsjs/react-with-addons]]
                 [cljsjs/react-with-addons "0.14.0-0"]
                 [bidi "1.22.1"]
                 [kibu/pushy "0.3.6"]

                 [cljs-ajax "0.5.1"]
                 [org.apache.httpcomponents/httpasyncclient "4.1"]
                 [org.apache.httpcomponents/httpcore "4.4.3"]

                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.cemerick/url "0.1.1"]
                 [sablono "0.4.0"
                  :exclusions [cljsjs/react cljsjs/react-with-addons]]
                 ; [cljsjs/codemirror "5.7.0-3"]

                 ; Dev
                 [devcards "0.2.1"]

                 ;; Database
                 [com.datomic/datomic-pro "0.9.5327"
                  :exclusions [joda-time]]

                 ;; Web
                 [org.immutant/web "2.1.1"]
                 [compojure "1.4.0"]
                 [liberator "0.13"]
                 ;; NOTE: this is installed in my local maven
                 ;; manually.
                 [com.cemerick/friend "0.2.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-middleware-format "0.7.0"]
                 [hiccup "1.0.5"]
                 [crypto-password "0.1.3"]
                 [cheshire "5.5.0"] ;;; JSON
                 [clj-time "0.11.0"]
                 [garden "1.3.0-SNAPSHOT"]

                 [ring/ring-devel "1.4.0"]

                 [prismatic/schema "1.0.3"]
                 ;; Test
                 [org.clojure/test.check "0.9.0"]
                 ]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username "brian@kanopi.io"
                                   :password "04fd123f-c4c5-4bb5-939b-18f41be8a18f"
                                   }}
  :source-paths ["src-cljc" "src"]
  :test-paths ["src-cljc" "src" "test-cljc" "test"]
  :main kanopi.main

  :plugins [[lein-marginalia "0.8.0"
             :exclusions [org.clojure/clojurescript org.clojure/clojure]
             ]
            [lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-1"
             ; :exclusions [org.clojure/core.async]
             ]]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/out" "resources/public/js/out_devcards"]

  :profiles {:dev
             {:jvm-opts ["-XX:MaxPermSize=128M"]
              :plugins [[lein-environ "1.0.1"]
                        [lein-ancient "0.6.6"
                         :exclusions [org.clojure/tools.reader]]]
              :dependencies [[org.clojure/tools.nrepl "0.2.12"]
                             [org.clojure/tools.namespace "0.2.11"]
                             [ring/ring-devel "1.4.0"]
                             [ring/ring-mock "0.3.0"]
                             [org.clojure/data.codec "0.1.0"]
                             [org.clojure/data.csv "0.1.3"]
                             
                             ;; use client lib for testing
                             [http-kit "2.1.19"]
                             ]
              :env {:dev true}
              :source-paths ["dev"]
              :repl-options {:init-ns user}}

             :devcards
             {:plugins [[lein-environ "1.0.1"]]
              :dependencies [[org.clojure/tools.nrepl "0.2.12"]
                             [org.clojure/tools.namespace "0.2.11"]
                             [ring/ring-devel "1.4.0"]
                             [ring/ring-mock "0.3.0"]
                             [org.clojure/data.codec "0.1.0"]
                             [org.clojure/data.csv "0.1.3"]
                             
                             ;; use client lib for testing
                             [http-kit "2.1.19"]
                             ]
              :env {:dev true}
              :source-paths ["dev"]
              :repl-options {:init-ns user}}}

  :figwheel {;:server-logfile "target/logs/figwheel.log"
             :css-dirs ["resources/public/css"]}

  :cljsbuild {:builds
              [
               ;; lein figwheel to run with auto-reloading
               ;; lein cljsbuild once to run otherwise
               {:id "dev"
                :source-paths ["src-cljc" "src-cljs"]
                :figwheel {:on-jsload "kanopi.core/reload-om"}
                :compiler {:output-to "resources/public/js/main.js"
                           :output-dir "resources/public/js/out"
                           ;; NOTE: yes leading slash here for a reason!
                           :asset-path "/js/out"
                           :main kanopi.core
                           :optimizations :none
                           :pretty-print true
                           :source-map "resources/public/js/source_map.js"}}

               ;; lein figwheel devcards to run
               {:id "devcards"
                :source-paths ["src-cljc" "src-cljs" "dev-cljs"]
                :figwheel {:devcards true}
                :compiler {:output-to "resources/public/js/main_devcards.js"
                           :output-dir "resources/public/js/out_devcards"
                           ;; NOTE: no leading slash here for a reason!
                           :asset-path "js/out_devcards"
                           :main kanopi.devcards
                           :optimizations :none
                           :pretty-print true
                           :source-map "resources/public/js/source_map_devcards.js"}}
               ]
              })
