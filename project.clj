(defproject guns.cli/optparse "1.1.0-SNAPSHOT"
  :url "https://github.com/guns/optparse-clj"
  :description "OptionParser for Clojure: Functional GNU-style command line options parser."
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :source-paths ["src" "src-cljs"]
  :aliases {"example" ["trampoline" "run" "-m" "example"]}
  :profiles {:dev {:source-paths ["src" "src-cljs" "src-example"]
                   :plugins [[lein-cljsbuild "0.3.2"]]
                   :cljsbuild {:builds {:dev {:source-paths ["src-cljs" "src-example"]
                                              :compiler {:output-to "target/example-dev.js"
                                                         :optimizations :simple
                                                         :pretty-print true
                                                         :target :nodejs}}
                                        :prod {:source-paths ["src-cljs" "src-example"]
                                               :compiler {:output-to "target/example-prod.js"
                                                          :optimizations :advanced
                                                          :pretty-print false
                                                          :target :nodejs
                                                          :externs ["src-example/externs/process.js"]}}}}}})
