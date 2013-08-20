(ns example
  (:require [guns.cli.optparse :refer [parse]]))

(defn -main [& argv]
  (prn argv (parse argv [["-h" "--help"] ["-f" "--foo"]])))

(set! *main-cli-fn* -main)
