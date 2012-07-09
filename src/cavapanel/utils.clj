(ns cavapanel.utils
  (:import [java.io File])
  (:require [future-contrib.java-utils :as jutils]
            [cavapanel.vars :as vars]))

;; function for swap! which simply takes the new value
(defn use-new [old new]
  new)

;; function used for reduce (built in or is a macro)
(defn or-all [a b]
  (or a b))

(defmulti is-int?
  "return true of the argument can be parsed as an integer"
  class)

(defmethod is-int? String [#^String s]
  (try
    (Integer/parseInt s)
    true
    (catch NumberFormatException nfe
      false)))

;; The following catches all the likely derived types
;; http://docs.oracle.com/javase/6/docs/api/java/lang/Number.html
(defmethod is-int? Number [i] true)
(defmethod is-int? :default [i] false)

(defmulti is-long?
  "return true of the argument can be parsed as a long"
  class)

(defmethod is-long? String [#^String s]
  (try
    (Long/parseLong s)
    true
    (catch NumberFormatException nfe
      false)))

(defmethod is-long? Number [i] true)

(defmethod is-long? :default [i] false)

(defmulti #^Long as-long
  "Interpret a String or a Long as an longeger. (emtpy/nil -> 0)"
  class)
(defmethod as-long nil [i] 0)
(defmethod as-long Number [i] i)
(defmethod as-long String [#^String s]
  (if (> (count s) 0)
    (Long. s)
    0))


(defn remove-nils [a]
  (filter (fn [e] (not (nil? e))) a))

;; convert a vector, seq to a list
(defn as-list [v]
  (for [e v] e))

(defn merge-lists
  ([] (list)) ;; return the empty list
  ([a] (if (seq? a)
         a
         (if a
           (list a) ;; promote an atom to a list
           (list)))) ;; nil becomes the empty list
  ([a b] (if (or (nil? a) (and (seq? a) (empty? a)))
           (if (or (nil? b) (and (seq? b) (empty? b)))
             (list)
             (merge-lists b))
           (if (or (nil? b) (and (seq? b) (empty? b)))
             (merge-lists a)
             (if (seq? a)
               (if (seq? b)
                 (let [b0 (first b)
                       b1 (rest b)
                       a1 (reverse (cons b0 (reverse a)))]
                   (if (empty? b1)
                     a1
                     (merge-lists a1 b1)))
                 (reverse (cons b (reverse a))))
               (if (seq? b)
                 (cons a b)
                 (list a b))))))
  ([a b & more]
     (apply merge-lists (cons (merge-lists a b) more))))

(defn str-nonnil [str]
  (if (nil? str) "" str))

(defn str-addline
  ([] "")
  ([lines] (str-nonnil lines))
  ([lines line] (if (empty? lines)
                  (str-nonnil line)
                  (if (empty? line)
                    lines
                    (str lines "\n" line))))
  ([lines line & more]
     (reduce str-addline (str-addline lines line) more)))

(defn str-addcomma
  ([] "")
  ([commas] (if (coll? commas)
              (reduce str-addcomma commas)
              (str-nonnil commas)))
  ([commas comma] (if (empty? commas)
                    (str-addcomma comma)
                    (if (empty? comma)
                      (str-addcomma commas)
                      (str (str-addcomma commas) ", " (str-addcomma comma)))))
  ([commas comma & more]
     (reduce str-addcomma (str-addcomma commas comma) more)))

(defn str-combine
  ([] "")
  ([combines] (if (coll? combines)
              (reduce str-combine combines)
              (str-nonnil combines)))
  ([combines combine] (if (empty? combines)
                    (str-combine combine)
                    (if (empty? combine)
                      (str-combine combines)
                      (str (str-combine combines) " " (str-combine combine)))))
  ([combines combine & more]
     (reduce str-combine (str-combine combines combine) more)))

;; force realizion of lazy seqs
(defn str-realize [coll]
  (if (seq? coll)
    (apply str (concat (list "(")
                       (interpose " " (for [c coll] (str-realize c)))
                       (list ")")))
    (str coll)))

(defn java2hash [hm]
  (let [keys (.keySet hm)]
    (apply merge
           (for [k keys] (hash-map (keyword k) (.get hm k))))))

(defn keywordize [hm]
  (apply merge
         (for [k (keys hm)] (hash-map (keyword k) (get hm k)))))

(defn keywordize-seq [s]
  (for [k s] (keyword k)))
(defn unkeywordize [hm]
  (apply merge
         (for [k (keys hm)] (hash-map (name k) (get hm k)))))

(defn unkeywordize-seq [s]
  (for [k s] (name k)))

;; operations on booleans

(defn is-true? [v]
  (or (true? v) (= v "true")))

(defn is-true-str? [v]
  (str (is-true? v)))

(defn boolean-map [hm]
  (apply merge
         (for [k (keys hm)]
           (hash-map k (is-true? (get hm k))))))

(defn boolean-str-map [hm]
  (apply merge
         (for [k (keys hm)]
           (hash-map k (is-true-str? (get hm k))))))

;; operations on booleans strings

(defn mixed-boolean? [v]
  (cond
   (or (true? v) (= v "true")) true
   (or (false? v) (= v "false")) false
   true v))

;; converts to String rep of Boolean (or String)
(defn mixed-boolean-str? [v]
  (str (mixed-boolean? v)))

(defn mixed-boolean-map [hm]
  (apply merge
         (for [k (keys hm)]
           (hash-map k (mixed-boolean? (get hm k))))))

(defn mixed-boolean-str-map [hm]
  (apply merge
         (for [k (keys hm)]
           (hash-map k (mixed-boolean-str? (get hm k))))))

;; operations on booleans strings or numbers

(defn mixed? [v]
  (let [is-long (is-long? v)
        is-int (and is-long (is-int? v))]
    (cond
     (nil? v) nil
     (or (true? v) (= v "true")) true
     (or (false? v) (= v "false")) false
     (and is-long (not is-int)) (as-long v)
     is-int (jutils/as-int v)
     true v))) ;; String

(defn mixed-str? [v]
  (str (mixed? v)))

(defn mixed-map [hm]
  (apply merge
         (for [k (keys hm)]
           (hash-map k (mixed? (get hm k))))))

(defn mixed-str-map [hm]
  (apply merge
         (for [k (keys hm)]
           (hash-map k (mixed-str? (get hm k))))))

;; FIXME result message side effects ===================================

(defn set-result-msg [msg]
  (swap! vars/*result-msg* use-new msg))

(defn add-result-msg [msg]
  (set-result-msg (str-addline @vars/*result-msg* msg)))

(defn result-msg []
  @vars/*result-msg*)

(defn set-verbose [verbose]
  (swap! vars/*verbose* use-new verbose))
  
(defn verbose? []
  @vars/*verbose*)

;; FIXME this is the way to capture and handle error conditions
;; that cannot be passed directly back to the caller (e.g. get-*)
(defn stderr-msg [msgs]
  (binding [*out* *err*]
    (apply println msgs)
    (flush)))

(defn verbose-msg [& msgs]
  (if (verbose?)
    (stderr-msg msgs)))

(defn error-msg [& msgs]
  (stderr-msg msgs))

(defn if-str [verbose & vals]
  (if verbose
    (apply str vals)))

(defn mkdirp [dir]
  (let [dirfile (new File dir)]
    (.mkdirs dirfile)))

(defn verbose-set [name theset]
  (verbose-msg name "=" (str-addcomma theset)))

(defn set-success [success]
  (swap! vars/*success* use-new success))

(defn success? []
  @vars/*success*)
