(defproject guns.cli/optparse "1.1.0-SNAPSHOT"
  :url "https://github.com/guns/optparse-clj"
  :description "OptionParser for Clojure: Functional GNU-style command line options parser."
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :aliases {"example" ["trampoline" "run" "-m" "example"]}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :source-paths ["src" "src-cljs"]
  ;; ClojureScript
  :profiles {:dev {:plugins [[lein-cljsbuild "0.3.2"]]
                   :cljsbuild {:builds {:dev {:source-paths ["src-cljs"]
                                              :compiler {:output-to "target/optparse-dev.js"
                                                         :optimizations :whitespace
                                                         :target :nodejs
                                                         :pretty-print true}}
                                        :prod {:source-paths ["src-cljs"]
                                               :compiler {:output-to "target/optparse-prod.js"
                                                          :optimizations :advanced
                                                          :pretty-print false
                                                          :target :nodejs}}}}}})
