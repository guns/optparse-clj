(ns guns.cli.optparse-test
  (:require [clojure.string :as string]
            [guns.cli.optparse :as o]
            [clojure.test :refer [deftest is testing]]))

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
    (is (= (o/tokenize-arguments required ["-a" "foo" "-b"] :in-order true)
           [[[:short-opt "-a"]] ["foo" "-b"]]))))

(deftest test-compile-opt-specs
  (testing ":default :parse-fn and :assert entries"
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
               :default-desc "80"
               :parse-fn pfn
               :assert-fn vfn
               :assert-msg "%s is not a valid port number"}]))))
  (testing "minimal entries"
    (is (= (o/compile-option-specs [[nil "--minimal"]])
           [{:kw :minimal
             :short-opt nil
             :long-opt "--minimal"
             :required nil
             :desc nil
             :default nil
             :default-desc ""
             :parse-fn nil
             :assert-fn nil
             :assert-msg nil}])))
  (testing "description is optional"
    (is (= (o/compile-option-specs [[nil "--foo ARG" :default "FOO"]])
           [{:kw :foo
             :short-opt nil
             :long-opt "--foo"
             :required "ARG"
             :desc nil
             :default "FOO"
             :default-desc "FOO"
             :parse-fn nil
             :assert-fn nil
             :assert-msg nil}])))
  (testing "fallback values"
    (is (= (set (map :default (o/compile-option-specs
                                [[nil "--alpha ARG"]
                                 [nil "--beta ARG" :default \β]]
                                :fallback ::undefined)))
           #{::undefined \β})))
  (testing "input validation"
    (is (thrown? AssertionError (o/compile-option-specs [[]])))
    (is (thrown? AssertionError (o/compile-option-specs [[nil nil]])))
    (is (thrown? AssertionError (o/compile-option-specs [[nil "--"]])))
    (is (thrown? AssertionError (o/compile-option-specs [[nil "--="]]))))
  (testing "option distinctness"
    (is (thrown? AssertionError (o/compile-option-specs [["-a" "--alpha"] ["-a" "--alice"]])))
    (is (thrown? AssertionError (o/compile-option-specs [["-a" "--alpha"] ["-b" "--alpha"]])))
    (is (o/compile-option-specs [[nil "--alpha"] [nil "--beta"]]))))

(deftest test-required-arguments
  (is (= (o/required-arguments
           [{:required "ARG" :short-opt nil :long-opt "--long"}
            {:required nil :short-opt "-s" :long-opt "--short"}
            {:required "FOO" :short-opt "-f" :long-opt "--foo"}])
         #{"--long" "-f" "--foo"})))

(deftest test-parse-option-tokens
  (let [specs [{:kw :port
                :short-opt "-p"
                :long-opt "--port"
                :required "NUMBER"
                :desc "Listen on this port"
                :default 80
                :default-desc "80"
                :parse-fn #(Integer/parseInt %)
                :assert-fn #(< 0 % 0x10000)
                :assert-msg "%s is not a valid port number"}
               {:kw :host
                :short-opt nil
                :long-opt "--host"
                :required "HOST"
                :desc nil
                :default "localhost"
                :default-desc "localhost"
                :parse-fn nil
                :assert-fn nil
                :assert-msg nil}
               {:kw :protocol
                :short-opt nil
                :long-opt "--protocol"
                :required "PROTO"
                :desc nil
                :default :tcp
                :default-desc "tcp"
                :parse-fn keyword
                :assert-fn nil
                :assert-msg nil}
               {:kw :help
                :short-opt "-h"
                :long-opt "--help"
                :required nil
                :desc nil
                :default nil
                :default-desc ""
                :parse-fn nil
                :assert-fn nil
                :assert-msg nil}]]
    (is (= (o/parse-option-tokens specs [[:short-opt "-p" "443"] [:long-opt "--host" "example.com"]])
           {:port 443 :host "example.com" :protocol :tcp :help nil}))
    (is (= (o/parse-option-tokens specs [[:long-opt "--protocol" "udp"]])
           {:port 80 :host "localhost" :protocol :udp :help nil}))
    (is (true? (:help (o/parse-option-tokens specs [[:long-opt "--help"]]))))
    (is (thrown? AssertionError (o/parse-option-tokens specs [[:long-opt "--port"]])))
    (is (thrown? AssertionError (o/parse-option-tokens specs [[:long-opt "--dwim"]])))
    (is (thrown-with-msg?
          AssertionError #"100000.*is not a valid port number"
          (o/parse-option-tokens specs [[:long-opt "--port" "100000"]])))
    (is (thrown-with-msg?
          AssertionError #"INVALID.*is not a valid port number"
          (o/parse-option-tokens specs [[:long-opt "--port" "INVALID"]])))))

(deftest test-summarize
  (is (= (o/summarize [{:kw :port
                        :short-opt "-p"
                        :long-opt "--port"
                        :required "NUMBER"
                        :desc "Listen on this port"
                        :default 80
                        :default-desc "80"
                        :parse-fn #(Integer/parseInt %)
                        :assert-fn #(< 0 % 0x10000)
                        :assert-msg nil}
                       {:kw :host
                        :short-opt nil
                        :long-opt "--host"
                        :required "HOSTNAME"
                        :desc "Hostname to bind to"
                        :default "localhost"
                        :default-desc "localhost"
                        :parse-fn nil
                        :assert-fn nil
                        :assert-msg nil}
                       {:kw :verbose
                        :short-opt "-v"
                        :long-opt "--verbose"
                        :required nil
                        :desc "Be verbose"
                        :default nil
                        :default-desc ""
                        :parse-fn keyword
                        :assert-fn nil
                        :assert-msg nil}])
         (string/join
           \newline
           ["  -p, --port NUMBER    80         Listen on this port"
            "      --host HOSTNAME  localhost  Hostname to bind to"
            "  -v, --verbose                   Be verbose"])))
  (is (= (o/summarize [{:kw :minimal
                        :short-opt nil
                        :long-opt "--minimal"
                        :required nil
                        :desc nil
                        :default nil
                        :default-desc ""
                        :parse-fn nil
                        :assert-fn nil
                        :assert-msg nil}])
         "      --minimal")))

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
            ["  -p, --port NUMBER      80         Listen on this port"
             "      --host HOST        localhost  Bind to this hostname"
             "  -l, --log-level LEVEL  1          Logging level"
             "      --protocol PROTO   tcp"
             "  -v, --verbose"
             "  -n, --noop"])]))
  (testing ":in-order"
    (is (= (o/parse ["-a" "α" "-b"]
                    [["-a" "--alpha"] ["-b" "--beta"]]
                    :in-order true)
           [{:alpha true :beta nil}
            ["α" "-b"]
            "  -a, --alpha\n  -b, --beta"])))
  (testing ":fallback"
    (is (= (o/parse []
                    [["-a" "--alpha ARG"] ["-b" "--beta ARG" :default \β]]
                    :fallback ::undefined)
           [{:alpha ::undefined :beta \β}
            []
            "  -a, --alpha ARG\n  -b, --beta ARG   β"]))))
