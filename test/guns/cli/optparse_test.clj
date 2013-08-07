(ns guns.cli.optparse-test
  (:require [clojure.string :as string]
            [guns.cli.optparse :as o]
            [clojure.test :refer [deftest is]]))

(deftest test-tokenize-arguments
  (let [required #{"-p" "--port" "--host" "-l" "--log-level" "--protocol"}]
    (is (= (o/tokenize-arguments required ["-abcp80"])
           [[[:short-opt "-a"] [:short-opt "-b"] [:short-opt "-c"] [:short-opt "-p" "80"]] []]))
    (is (= (o/tokenize-arguments required ["--port=80" "--host" "example.com"])
           [[[:long-opt "--port" "80"] [:long-opt "--host" "example.com"]] []]))
    (is (= (o/tokenize-arguments required ["--foo=bar" "--noopt="])
           [[[:long-opt "--foo" "bar"] [:long-opt "--noopt" ""]] []]))
    (is (= (o/tokenize-arguments required ["-a" "--" "-b"])
           [[[:short-opt "-a"]] ["-b"]]))
    (is (= (o/tokenize-arguments required ["-a" "foo" "-b"])
           [[[:short-opt "-a"] [:short-opt "-b"]] ["foo"]]))
    (is (= (o/tokenize-arguments required ["-a" "foo" "-b"] :trailing-options false)
           [[[:short-opt "-a"]] ["foo" "-b"]]))))

(deftest test-compile-opt-specs
  (let [pfn #(Integer/parseInt %)
        vfn #(< 0 % 0x10000)]
    (is (= (o/compile-option-specs
             [["-p" "--port NUMBER" "Listen on this port"
               :default 80
               :parse-fn pfn
               :assert [vfn "%s is not a valid port number"]]])
           [{:kw :port
             :short-opt "-p"
             :long-opt "--port"
             :required "NUMBER"
             :desc "Listen on this port"
             :default 80
             :parse-fn pfn
             :assert-fn vfn
             :assert-msg "%s is not a valid port number"}]))
    (is (= (o/compile-option-specs [[nil "--minimal" nil]])
           [{:kw :minimal
             :short-opt nil
             :long-opt "--minimal"
             :required nil
             :desc nil
             :default nil
             :parse-fn nil
             :assert-fn nil
             :assert-msg nil}]))))

(deftest test-required-arguments
  (is (= (o/required-arguments
           [{:required "ARG" :short-opt nil :long-opt "--long"}
            {:required nil :short-opt "-s" :long-opt "--short"}
            {:required "FOO" :short-opt "-f" :long-opt "--foo"}])
         #{"--long" "-f" "--foo"})))

(deftest test-process-option-tokens
  (let [specs [{:kw :port
                :short-opt "-p"
                :long-opt "--port"
                :required "NUMBER"
                :desc "Listen on this port"
                :default 80
                :parse-fn #(Integer/parseInt %)
                :assert-fn #(< 0 % 0x10000)
                :assert-msg "%s is not a valid port number"}
               {:kw :host
                :short-opt nil
                :long-opt "--host"
                :required "HOST"
                :desc nil
                :default "localhost"
                :parse-fn nil
                :assert-fn nil
                :assert-msg nil}
               {:kw :protocol
                :short-opt nil
                :long-opt "--protocol"
                :required "PROTO"
                :desc nil
                :default :tcp
                :parse-fn keyword
                :assert-fn nil
                :assert-msg nil}
               {:kw :help
                :short-opt "-h"
                :long-opt "--help"
                :required nil
                :desc nil
                :default nil
                :parse-fn nil
                :assert-fn nil
                :assert-msg nil}]]
    (is (= (o/process-option-tokens specs [[:short-opt "-p" "443"] [:long-opt "--host" "example.com"]])
           {:port 443 :host "example.com" :protocol :tcp :help nil}))
    (is (= (o/process-option-tokens specs [[:long-opt "--protocol" "udp"]])
           {:port 80 :host "localhost" :protocol :udp :help nil}))
    (is (true? (:help (o/process-option-tokens specs [[:long-opt "--help"]]))))
    (is (thrown? AssertionError (o/process-option-tokens specs [[:long-opt "--port"]])))
    (is (thrown? AssertionError (o/process-option-tokens specs [[:long-opt "--dwim"]])))
    (is (thrown-with-msg?
          AssertionError #"100000.*is not a valid port number"
          (o/process-option-tokens specs [[:long-opt "--port" "100000"]])))
    (is (thrown-with-msg?
          AssertionError #"INVALID.*is not a valid port number"
          (o/process-option-tokens specs [[:long-opt "--port" "INVALID"]])))))

(deftest test-summarize
  (is (= (o/summarize [{:kw :port
                        :short-opt "-p"
                        :long-opt "--port"
                        :required "NUMBER"
                        :desc "Listen on this port"
                        :default 80
                        :parse-fn #(Integer/parseInt %)
                        :assert-fn #(< 0 % 0x10000)
                        :assert-msg nil}
                       {:kw :host
                        :short-opt nil
                        :long-opt "--host"
                        :required "HOSTNAME"
                        :desc "Hostname to bind to"
                        :default "localhost"
                        :parse-fn nil
                        :assert-fn nil
                        :assert-msg nil}
                       {:kw :protocol
                        :short-opt "-v"
                        :long-opt "--verbose"
                        :required nil
                        :desc "Be verbose"
                        :default nil
                        :parse-fn keyword
                        :assert-fn nil
                        :assert-msg nil}])
         (string/join
           \newline
           ["Options:"
            "  -p, --port NUMBER    80         Listen on this port"
            "      --host HOSTNAME  localhost  Hostname to bind to"
            "  -v, --verbose                   Be verbose"]))))

(deftest test-parse
  (is (= (o/parse ["-p443" "--host=example.com" "--log-level" "2" "--protocol" "udp"
                   "arg" "--verbose" "--" "-n" "--noop" "foo" "bar"]
                  [["-p" "--port NUMBER" "Listen on this port"
                    :parse-fn #(Integer/parseInt %)
                    :default 80]
                   [nil "--host HOST" "Bind to this hostname"
                    :default "localhost"]
                   ["-l" "--log-level LEVEL" "Logging level"
                    :parse-fn #(Integer/parseInt %)
                    :default 1]
                   [nil "--protocol PROTO" nil
                    :parse-fn keyword
                    :default :tcp]
                   ["-v" "--verbose" nil]
                   ["-n" "--noop"]])
         [{:port 443
           :host "example.com"
           :log-level 2
           :protocol :udp
           :verbose true
           :noop nil}
          ["arg" "-n" "--noop" "foo" "bar"]
          (string/join
            \newline
            ["Options:"
             "  -p, --port NUMBER      80         Listen on this port"
             "      --host HOST        localhost  Bind to this hostname"
             "  -l, --log-level LEVEL  1          Logging level"
             "      --protocol PROTO   tcp"
             "  -v, --verbose"
             "  -n, --noop"])])))