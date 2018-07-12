(ns net.icbink.expand-headers.core
  (:require [clojure.string :as str]))


(defn- extract-nonletters [s]
  (filter (fn [[_ c]] (= (str/lower-case c) (str/upper-case c)))
          (map-indexed vector s)))

(defn- reinsert-chars* [accum src i0 pairs]
  (if (empty? pairs)
    (concat accum src)
    (let [[i c] (first pairs)
          [before after] (split-at (- i i0) src)]
      (recur (concat accum before [c])
             after
             (+ i 1)
             (rest pairs)))))

(defn- reinsert-chars [pairs s]
  (reinsert-chars* '() s 0 pairs))

(defn- warp-letters-case [n s]
  (when (<= (Math/pow 2 (count s)) n)
    ;; XXX try adding spaces?
    (throw (RuntimeException. "Not enough characters!")))
  (map-indexed
   (fn [i c]
     ((if (= 0 (bit-and n (bit-shift-left 1 i)))
        str/lower-case
        str/upper-case)
      c))
   s))

(defn expand-header [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k v :as input]]
     (if (string? v)
       (xf result input)
       (let [nonletters (extract-nonletters k)
             letters (remove (into #{} (map second nonletters)) k)
             warp-case (fn [n]
                         (->> letters
                              (warp-letters-case n)
                              (reinsert-chars nonletters)
                              (apply str)))]
         (reduce xf result (map-indexed (fn [i s]
                                          [(warp-case i) s])
                                        v)))))))

(defn expand-headers [m]
  (into {} expand-header m))

(defn wrap-expand-headers
  [handler]
  (fn [req]
    (update (handler req) :headers expand-headers)))

(comment

  (clojure.pprint/pprint
   (expand-headers {"a-bcd-efgh-ijkl-mnop-qrst" ["1" "2" "3" "4" "5"]
                    "cAse-lEfT-ALONE-wHeN" "value is a string"})
   )
  )
