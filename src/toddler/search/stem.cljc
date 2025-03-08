(ns toddler.search.stem
  (:require [clojure.string :as str]))

;; Define vowels
(def vowels #{"a" "e" "i" "o" "u"})

(defn vowel? [ch]
  (contains? vowels ch))

(defn consonant? [ch]
  (not (vowel? ch)))

(defn double-consonant? [word]
  (when word
    (let [len (count word)
          [f s] (reverse word)]
      (when (and (> len 1)
                 (= f s)
                 (consonant? f))
        word))))

;; Measure 'm': Number of vowel-consonant sequences
(defn count-m [word]
  (count (re-seq #"[aeiou]+[^aeiou]+" word)))

(defn *o [word]
  (when (and word (re-find #"[^aeuio][aeiou][^aeiouwxy]$" word))
    word))

(defn not-*o
  [word]
  (when (and word (nil? *o)) word))

(defn *v [word]
  (when (re-find #"[aeiou]" word) word))

(defn ends-with [suffix]
  (fn [word]
    (when word
      (if (str/ends-with? word suffix)
        (subs word 0 (- (count word) (count suffix)))
        nil))))

(defn m [pred n]
  (fn [word]
    (when (and word (pred (count-m word) n))
      word)))

(defn single-letter
  [word]
  (subs word 0 (dec (count word))))

(defn match [regex]
  (fn [word]
    (when (and word (re-find regex word)) word)))

(defn add
  [suffix]
  (fn [word]
    (if (empty? suffix) word
        (str word suffix))))

(defn process
  [word rules]
  (letfn [(->idx [rule]
            (.indexOf rules rule))]
    (if-some [[replacement idx] (reduce
                                 (fn [_ [p r :as rule]]
                                   (when-let [w (p word)]
                                     (cond
                                       (= identity r) (reduced nil)
                                       (string? r) (reduced [(str w r) (->idx rule)])
                                       (fn? r) (reduced [(r w) (->idx rule)])
                                       :else
                                       (throw
                                        (ex-info "Invalid replacement"
                                                 {:predicate p
                                                  :word word
                                                  :replacement r})))))
                                 nil
                                 rules)]
      [true replacement idx]
      [false word nil])))
