(ns example
  "An example program with global and subcommand options."
  (:require [clojure.string :as string]
            [guns.cli.optparse :refer [parse]])
  (:import (java.util.regex Pattern))
  (:gen-class))

(def version "1.0.0")

(def global-options
  [["-h" "--help"]
   ["-V" "--version"]])

(def server-options
  [["-p" "--port NUMBER" "Listen on this port"
    :default 8080
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number"]]
   [nil "--host HOST" "Bind to this hostname"
    :default "localhost"]
   ["-d" "--detach" "Detach and run in the background"]
   ["-h" "--help"]])

(defn unindent
  "Unindent multiline string by the first non-zero indent."
  [s]
  (if-let [indent (re-find #"(?m)^[ \t]+" s)]
    (string/replace s (Pattern/compile (format "(?m)^%s" indent)) "")
    s))

(def global-usage
  (unindent
    "Usage: example [options] command [command-options]

     Commands:
       server   Start an example server

     Options:
     %s

     See `example command --help` for more information on each command."))

(def server-usage
  (unindent
    "Usage: example server [options]

     Listen for requests, do something, and return some data.

     Options:
     %s"))

(defn exit
  ([status] (System/exit status))
  ([status msg] (println msg) (System/exit status)))

(defn server-cmd
  "Run server with arguments, returning an exit status."
  [argv]
  (let [[opts args summary] (parse argv server-options)]
    (if (:help opts)
      (println (format server-usage summary))
      (do (println (format "Listening on %s:%d" (:host opts) (:port opts)))
          (when (:detach opts) (println "Detaching from terminal!"))
          (println "Goodbye!")))))

(defn -main [& argv]
  (try
    (let [[opts args summary] (parse argv global-options :in-order true)]
      ;; Early exit conditions
      (cond (:help opts) (exit 0 (format global-usage summary))
            (:version opts) (exit 0 version))
      ;; Run a subcommand
      (case (first args)
        "server" (server-cmd (rest args))
        (exit 1 (format global-usage summary))))
    (catch AssertionError e
      (exit 1 (.getMessage e)))))
