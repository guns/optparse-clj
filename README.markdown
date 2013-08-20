```
             _                                       _ _
            | |                                     | (_)
  ___  _ __ | |_ _ __   __ _ _ __ ___  ___       ___| |_
 / _ \| '_ \| __| '_ \ / _` | '__/ __|/ _ \ ___ / __| | |
| (_) | |_) | |_| |_) | (_| | |  \__ \  __/|___| (__| | |
 \___/| .__/ \__| .__/ \__,_|_|  |___/\___|     \___|_| |
      | |       | |                                  _/ |
      |_|       |_|                                 |__/
```

OptionParser for Clojure and ClojureScript. Works like [clojure.tools.cli][],
but supports GNU option parsing conventions:

<https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html>

## Installation

[Leiningen][] dependency:

```clojure
[guns.cli/optparse "1.1.0"]
```

## Features

* Short options clumping:

  The command line argument `-abc` is interpreted as `-a -b -c`.
  If the `-b` switch requires an argument, it is interpreted instead as `-a -b "c"`.

* Long options with `=`:

  Both `--foo=bar` and `--foo bar` are supported.

  The form `--foo=` with no trailing optarg is interpreted as `--foo ""`.

* Trailing options

  Options can occur before and after arguments by default:

  `-a foo bar -bc` is interpreted as `-a -b -c foo bar`.

  The `parse` function accepts an option `:in-order true` that disables this
  behaviour. This is primarily useful for creating commands that accept
  subcommands, like the `git` program.

  `--` forcibly stops options processing so that all following words are added
  to the argument stack.

## Option specification

Option vectors are composed of:

```clojure
[short-opt long-opt description
 :keyword value …]

;; Examples

["-v" "--version" "Print version string"]

["-p" "--port NUMBER" "Listen on given port"
 :default 8080
 :parse-fn #(Integer/parseInt %)
 :assert [#(< 0 % 0x10000) "%s is not a valid port number"]]
```

`short-opt` and `description` are optional, and may be passed as `nil`.

`long-opt` is mandatory and maps to keywordized keys in the resulting options
map.

Long options are boolean toggles by default, with a default value of `nil`.

If the long-opt string contains an example argument like `"--port NUMBER"` or
`"--host=HOSTNAME"` (the equals sign is optional), the option is interpreted
as requiring an argument.

The following option pairs are supported in option vectors:

Key             | Value
--------------- | ----------------------------------------------------------------
`:key`          | The key to use in the options map; defaults to `long-opt` keywordized
`:default`      | Default value of option
`:default-desc` | A string representing the default value in the summary; defaults to the string representation of `:default`
`:parse-fn`     | A function that receives the required option argument string and returns the interpreted value
`:assert`       | A vector of `[assert-fn assert-msg]`.

`assert-fn` is a predicate that takes the required option value (after
processing by `:parse-fn`) and returns true/false.

`assert-msg` is the message used when throwing an AssertionError when
`:assert-fn` returns false. It may contain a single `%s` format specifier that
will be replaced with the optval.

## Examples

The following examples are for Clojure, but optparse-clj also works in
ClojureScript.

Given:

```clojure
(ns example
  (:require [guns.cli.optparse :refer [parse]])
  (:import (java.net InetAddress))
  (:gen-class))

(def options
  [["-p" "--port NUMBER" "Listen on this port"
    :default 8080
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number"]]
   [nil "--host HOST" "Bind to this hostname"
    :default-desc "localhost"
    :default (InetAddress/getByName "localhost")
    :parse-fn #(InetAddress/getByName %)]
   ["-d" "--detach" "Detach and run in the background"]
   ["-h" "--help"]])
```

Parsing an argument vector from the command line:

```clojure
(parse ["command" "-dp80" "--host=example.com" "subcommand"] options)
```

Returns a vector of `[options-map remaining-args options-summary]`:

```clojure
[{:help nil,
  :detach true,
  :host #<Inet4Address example.com/93.184.216.119>,
  :port 80}

 ["command" "subcommand"]

 "  -p, --port NUMBER  8080       Listen on this port
        --host HOST    localhost  Bind to this hostname
    -d, --detach                  Detach and run in the background
    -h, --help"]
```

A complete example program with both global and subcommand options handling
can be found in [src-example/example.clj][], and may also be tested from the
command line:

```
$ lein example server -dp80 --host=example.com
Listening on example.com:80
Detaching from terminal!
Goodbye!
```

A ClojureScript example is available at [src-example/example.cljs][].

## TODO

- [ ] Support multiple invocations of an option:
      `-vvv` or `--hook "touch timestamp" --hook "notify-send DONE"`

## Inspired by:

* [clojure.tools.cli][]
* [OptionParser][]

## LICENSE

```
The MIT License (MIT)

Copyright (c) 2013 Sung Pae <self@sungpae.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

[clojure.tools.cli]: https://github.com/clojure/tools.cli
[src-example/example.clj]: src-example/example.clj
[src-example/example.cljs]: src-example/example.cljs
[Leiningen]: http://leiningen.org/
[OptionParser]: http://ruby-doc.org/stdlib-2.0/libdoc/optparse/rdoc/OptionParser.html
