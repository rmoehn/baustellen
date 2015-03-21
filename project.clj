(defproject baustellen "0.1.0-SNAPSHOT"
  :description "Primitive resource allocation with tabu search"
  :url "https://github.com/rmoehn/baustellen"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [amalloy/ring-buffer "1.0"]
                 [rhizome "0.2.1"]
                 [enlive "1.1.5"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]]
                   :source-paths ["dev"]}})
