(defproject guns.cli/optparse "1.1.2-SNAPSHOT"
  :url "https://github.com/guns/optparse-clj"
  :description "OptionParser for Clojure and ClojureScript: Functional GNU-style command line options parser."
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1896"]]
  :profiles {:dev {:source-paths ["src-example"]
                   :aliases {"example" ["trampoline" "run" "-m" "example"]
                             "cljs" ["do" "cljx" "once," "cljsbuild" "once"]
                             "build" ["do" "cljx" "once," "jar"]}
                   :plugins [[com.keminglabs/cljx "0.3.0"]
                             [lein-cljsbuild "0.3.2"]]
                   :cljx {:builds [{:source-paths ["src-cljx"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   {:source-paths ["src-cljx"]
                                    :output-path "target/classes"
                                    :rules :cljs}]}
                   ;; JS builds for local testing only
                   :cljsbuild {:builds [{:source-paths ["src-example"]
                                         :compiler {:output-to "target/example-dev.js"
                                                    :optimizations :simple
                                                    :pretty-print true
                                                    :target :nodejs}}
                                        {:source-paths ["src-example"]
                                         :compiler {:output-to "target/example.js"
                                                    :optimizations :advanced
                                                    :pretty-print false
                                                    :target :nodejs
                                                    :externs ["src-example/externs/process.js"]}}]}}})
