(ns example
  (:require [guns.cli.optparse :refer [parse]]))

(defn -main [& argv]
  (let [[opts args summary] (parse argv [["-h" "--help"] ["-f" "--foo"]])]
    (prn opts args summary)))

(set! *main-cli-fn* -main)
