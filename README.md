# expand-headers

Expand each ring response header with seq-of-String values into separate map
entries with string values and keys differentiated by case.

## Why

The [ring spec](https://github.com/ring-clojure/ring/blob/master/SPEC#L117)
defines the format of Clojure response headers as (emphasis mine)

> A Clojure map of HTTP header names to header values. These values may be
> either Strings, in which case one name/value header will be sent in the HTTP
> response, **or a seq of Strings, in which case a name/value header will be sent for each such String value**.

This may cause problems for ring apps exposed as AWS API Gateway-proxied Lambdas
in datomic ions. The Lambda Proxy Integration [does not
accept](https://stackoverflow.com/questions/46596143/use-multiple-same-header-field-in-lambda-proxy-integration)
a list of strings as the value for a header. For example, the ring session
middleware breaks because of this if you use the [cookie session
store](https://github.com/ring-clojure/ring/wiki/Sessions#session-stores).

Since browsers treat HTTP headers as case-insensitive, a workaround is to
separate each entry in the original map into a different key/value pair in the
headers map, with keys differentiated by case. For example,

```clojure
(require '[net.icbink.expand-headers.core :refer (expand-headers)])

(expand-headers {"a-bcd-efgh-ijkl-mnop-qrst" ["1" "2" "3" "4" "5"]
                 "cAse-lEfT-ALONE-wHeN" "value is a string"})

;=>

{"a-bcd-efgh-ijkl-mnop-qrst" "1",
 "A-bcd-efgh-ijkl-mnop-qrst" "2",
 "a-Bcd-efgh-ijkl-mnop-qrst" "3",
 "A-Bcd-efgh-ijkl-mnop-qrst" "4",
 "a-bCd-efgh-ijkl-mnop-qrst" "5",
 "cAse-lEfT-ALONE-wHeN" "value is a string"}
```

This library implements this workaround as a ring wrapper.

## Usage

Add this repo to your deps.edn as a [git
dependency](https://clojure.org/news/2018/01/05/git-deps), then wrap your ring
app with `wrap-expand-headers`. I think it's safe to add it as the outermost
wrapper (i.e., last if you use the `->` threading macro), but in any case it
must contain all other wrappers that may introduce headers with seq-of-String
values.

```clojure

(require '[net.icbink.expand-headers.core :refer (wrap-expand-headers)])

(defn ring-handler [_]
  {:status 200
   :headers {"a-bcd-efgh-ijkl-mnop-qrst" ["1" "2" "3" "4" "5"]
             "cAse-lEfT-ALONE-wHeN" "value is string"}}
   :body "This better work...")

(def ring-app
  (-> ring-handler
      ;; other ring wrappers here
      wrap-expand-headers))
```

## Status

Beta; API is unlikely to change in breaking ways but this has only been tested
in the REPL so far.

Known limitation: an exception will be raised if you have a header with more
values to expand than 2^n where n is the number of letters in the header key
(i.e., not including numbers, punctuation, or other characters that don't have a
case).

HTH!

## License

This is distributed under the [Eclipse Public License
1.0](http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
