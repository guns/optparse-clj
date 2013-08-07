(ns guns.cli.optparse
  (:require [clojure.string :as string]))

(defn tokenize-arguments
  "Reduce arguments sequence into a tuple of [opt-type opt optarg] vectors and
   a vector of remaining arguments. Returns as [options remaining-args].

   Expands clumped short options like \"-abc\" into:
   [[:short-opt \"-a\"] [:short-opt \"-b\"] [:short-opt \"-c\"]]

   If \"-b\" were in the set of options that require arguments, \"-abc\" would
   then be interpreted as: [[:short-opt \"-a\"] [:short-opt \"-b\" \"c\"]]

   Long options with `=` are always parsed as option + optarg, even if nothing
   follows the `=` sign.

   If :trailing-options is false, the first non-option, non-optarg argument
   stops options processing. This is useful for handling subcommand options."
  [required argv & opts]
  (let [{:keys [trailing-options] :or {trailing-options true}} opts]
    (loop [opts [] args [] [car & cdr] argv]
      (if car
        (condp re-seq car
          ;; Double dash always ends options processing
          #"\A--\z" (recur opts (into args cdr) [])
          ;; Long options with assignment always passes optarg, required or not
          #"\A--.+=" (recur (conj opts (into [:long-opt] (string/split car #"=" 2)))
                            args cdr)
          ;; Long options, consumes cdr head if needed
          #"\A--" (let [[[optarg] cdr] (if (required car)
                                         (split-at 1 cdr)
                                         [nil cdr])]
                    (recur (conj opts (into [:long-opt car] (if optarg [optarg] [])))
                           args cdr))
          ;; Short options, expands clumped opts until an optarg is required
          #"\A-." (let [characters (take-while seq (iterate rest (seq (.substring car 1))))
                        [os cdr] (reduce (fn [[os tail] [c & cs]]
                                           (let [o (str \- c)]
                                             (if (required o)
                                               (reduced
                                                 (if (seq cs)
                                                   ;; Get optarg from rest of car
                                                   [(conj os [:short-opt o (string/join cs)]) tail]
                                                   ;; Get optarg from head of cdr
                                                   [(conj os [:short-opt o (first tail)]) (rest tail)]))
                                               [(conj os [:short-opt o]) tail])))
                                         [[] cdr] characters)]
                    (recur (into opts os) args cdr))
          (if trailing-options
            (recur opts (conj args car) cdr)
            (recur opts (into args (cons car cdr)) [])))
        [opts args]))))

(defn compile-option-specs
  "Convert option vectors into a vector of complete option specifications."
  [options]
  (mapv (fn [[short-opt long-opt desc & {:keys [default parse-fn assert]}]]
          (let [[assert-fn assert-msg] assert
                [_ opt req] (re-find #"--([^ =]+)(?:[ =](.*))?" long-opt)]
            {:kw (keyword opt)
             :short-opt short-opt
             :long-opt (str "--" opt)
             :required req
             :desc desc
             :default default
             :parse-fn parse-fn
             :assert-fn assert-fn
             :assert-msg assert-msg}))
        options))

(defn required-arguments
  "Extract set of short and long options that require arguments."
  [specs]
  (reduce (fn [s {:keys [required short-opt long-opt]}]
            (if required
              (cond-> s
                short-opt (conj short-opt)
                long-opt (conj long-opt))
              s))
          #{} specs))

(defn process-option-tokens
  "Reduce sequence of [opt-type opt optarg] tuples into a map of options
   merged over the default values according to option specifications.

   Throws AssertionError on invalid options, missing required arguments,
   option argument parsing exceptions, and validation failures."
  [specs opt-tokens]
  (let [defaults (reduce (fn [m sp] (assoc m (:kw sp) (:default sp)))
                         {} specs)]
    (reduce (fn [m [otype opt arg]]
              (let [spec (first (filter #(= opt (otype %)) specs))
                    {:keys [kw required parse-fn assert-fn assert-msg]} spec
                    assert-msg (or assert-msg "Invalid option argument: `%s`")
                    _ (do (assert spec (str "Invalid option: " (pr-str opt)))
                          (when required
                            (assert arg (format "Option %s missing required argument %s"
                                                (pr-str opt)
                                                (pr-str required)))))
                    value (let [v (if required arg true)]
                            (if parse-fn
                              (try (parse-fn v)
                                   (catch Throwable _
                                     (assert false (format assert-msg v))))
                              v))]
                (when assert-fn
                  (assert (assert-fn value) (format assert-msg value)))
                (assoc m kw value)))
            defaults opt-tokens)))

(defn summarize
  "Reduce options specs into a options summary for printing at a terminal."
  [specs]
  (let [parts (map (fn [{:keys [short-opt long-opt required default desc]}]
                     [(let [s "  "
                            s (if short-opt
                                (str s short-opt ", ")
                                (str s "    "))
                            s (str s long-opt)]
                        (if required (str s \space required) s))
                      (if (and required default)
                        (if (keyword? default) (name default) (str default))
                        "")
                      (or desc "")])
                   specs)
        [optlen vallen] (reduce (fn [[olen vlen] [o v _]]
                                  [(max olen (count o)) (max vlen (count v))])
                                [0 0] parts)
        fmt (str "%-" (inc optlen) "s %-" (inc vallen) "s %s")
        lines (map #(string/trimr (apply format fmt %)) parts)]
    (string/join \newline lines)))

(defn parse
  "Command line options parser. Works like clojure.tools.cli, with some
   features from Ruby's OptionParser.

   GNU style short option clumping is supported, as well as long option
   arguments following an equal sign. Trailing options are also supported, and
   can be turned off by calling with :trailing-options false.

   Example:

     (parse argv
            [[\"-p\" \"--port NUMBER\" \"Listen on this port\"
              :parse-fn #(Integer/parseInt %)
              :assert [#(< 0 % 0x10000) \"%s is not a valid port number\"]]
             [nil \"--host HOST\" \"Bind to this hostname\"
              :default \"localhost\"]
             [\"-v\" \"--verbose\" nil]]
            :trailing-options false ; Defaults to true
            )

   Short options are optional, but long options are required and map to
   keywordized keys in the resulting options map.

   If a long option is followed by a space (or =) and an example argument
   string, an option argument will be required and passed to :parse-fn. The
   resulting value is validated with :assert, throwing an AssertionError on
   failure.

   Otherwise, options are assumed to be boolean flags, defaulting to false.
   \"--[no-]option\" variations are not implicitly supported.

   Returns [options-map remaining-args options-summary]"
  [argv [& options] & opts]
  (let [specs (compile-option-specs options)
        req (required-arguments specs)
        {:keys [trailing-options] :or {trailing-options true}} opts
        [opt-tokens rest-args] (tokenize-arguments
                                 req argv
                                 :trailing-options trailing-options)]
    [(process-option-tokens specs opt-tokens) rest-args (summarize specs)]))
