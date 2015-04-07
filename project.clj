(defproject nebula "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [environ "1.0.0"]
                 [leiningen "2.5.0"]
                 [com.datomic/datomic-free "0.9.5153"]
                 ]
  :main nebular.main
  :plugins [[lein-marginalia "0.8.0"]]
  :profiles {:dev {:plugins [[lein-environ "1.0.0"]
                             [lein-ancient "0.6.2"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.9"]]
                   :env {:dev true}
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}})
