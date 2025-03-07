(ns toddler.search.en
  (:require
   [clojure.string :as str]
   [toddler.search.stem
    :refer [ends-with single-letter
            *o *v count-m add match process m
            not-*o double-consonant?]]))

(let [rules [[(ends-with "sses") "ss"]
             [(ends-with "ies") "i"]
             [#(re-find #"ss$" %) identity]
             [(ends-with "s") ""]]]
  (defn step-1a
    [word]
    (let [[_ word] (process word rules)]
      word)))

(comment
  (def word "ponies")
  (def suffix "sses")
  ((ends-with "sses") "ponies")
  (step-1a "caresses")
  (step-1a "caress")
  (step-1a "ponies")
  (step-1a "ties")
  (step-1a "cats"))

(let [rules1 [[(comp (ends-with "eed") (m > 0)) (add "ee")]
              [(comp (ends-with "ed") *v) (add "")]
              [(comp (ends-with "ing") *v) (add "")]]
      rules2 [[(match #"at$") (add "e")]
              [(match #"bl$") (add "e")]
              [(match #"iz$") (add "e")]
              [(comp
                (match #"[^lsz]$")
                double-consonant?)
               single-letter]
              [(comp *o (m = 1)) (add "e")]]]
  (defn step-1b
    [word]
    (let [[processed? word1 idx]
          (process word rules1)]
      (cond
        (not processed?) word
        (pos? idx) (let [[_ word2]
                         (process word1 rules2)]
                     word2)
        :else word1))))

(comment
  (step-1b "conflated")
  (step-1b "troubled")
  (step-1b "hopping")
  (step-1b "tanned")
  (step-1b "falling")
  (step-1b "failing")
  (step-1b "hissing")
  (step-1b "filing"))

(let [rules [[(comp (ends-with "y") *v) (add "i")]]]
  (defn step-1c
    [word]
    (second (process word rules))))

(comment
  ((ends-with "y") "happy")
  (step-1c "happy")
  (step-1c "sky"))

(def step-1 (comp step-1c step-1b step-1a))

(comment
  (step-1 "conflated"))

(let [rules [[(ends-with "ational") (add "ate")]
             [(ends-with "tional") (add "tion")]
             [(ends-with "enci") (add "ence")]
             [(ends-with "anci") (add "ance")]
             [(ends-with "izer") (add "ize")]
             [(ends-with "abli") (add "able")]
             [(ends-with "alli") (add "al")]
             [(ends-with "entli") (add "ent")]
             [(ends-with "eli") (add "e")]
             [(ends-with "ousli") (add "ous")]
             [(ends-with "ization") (add "ize")]
             [(ends-with "ation") (add "ate")]
             [(ends-with "ator") (add "ate")]
             [(ends-with "alism") (add "al")]
             [(ends-with "iveness") (add "ive")]
             [(ends-with "fulness") (add "ful")]
             [(ends-with "ousness") (add "ous")]
             [(ends-with "aliti") (add "al")]
             [(ends-with "iviti") (add "ive")]
             [(ends-with "biliti") (add "ble")]]]
  (defn step-2
    [word]
    (if-not (pos? (count-m word))
      word
      (second (process word rules)))))

(comment
  (step-2 "relational")
  (step-2 "conditional")
  (step-2 "valenci")
  (step-2 "digitizer")
  (step-2 "conformabli")
  (step-2 "decisiveness"))

(let [rules [[(ends-with "icate") (add "ic")]
             [(ends-with "ative") (add "")]
             [(ends-with "alize") (add "al")]
             [(ends-with "iciti") (add "ic")]
             [(ends-with "ical") (add "ic")]
             [(ends-with "ful") (add "")]
             [(ends-with "ness") (add "")]]]
  (defn step-3
    [word]
    (if-not (pos? (count-m word))
      word
      (second (process word rules)))))

(comment
  (map step-3
       ["triplicate"
        "formative"
        "formalize"
        "electriciti"
        "electrical"
        "hopeful"
        "goodness"]))

(let [rules [[(ends-with "al") (add "")]
             [(ends-with "ance") (add "")]
             [(ends-with "ence") (add "")]
             [(ends-with "er") (add "")]
             [(ends-with "ic") (add "")]
             [(ends-with "able") (add "")]
             [(ends-with "ible") (add "")]
             [(ends-with "ant") (add "")]
             [(ends-with "ement") (add "")]
             [(ends-with "ment") (add "")]
             [(ends-with "ent") (add "")]
             [(fn [word]
                (if (re-find #"[st]ion" word)
                  (subs word 0 (- (count word) 3))
                  nil))
              (add "")]
             [(ends-with "ou") (add "")]
             [(ends-with "ism") (add "")]
             [(ends-with "ate") (add "")]
             [(ends-with "iti") (add "")]
             [(ends-with "ous") (add "")]
             [(ends-with "ive") (add "")]
             [(ends-with "ize") (add "")]]]
  (defn step-4
    [word]
    (if-not (> (count-m word) 1)
      word
      (second (process word rules)))))

(comment
  (map
   step-4
   ["revival"
    "allowance"
    "inference"
    "airliner"
    "gyroscopic"
    "adjustable"
    "defensible"
    "irritant"
    "replacement"
    "adjustment"
    "dependent"
    "adoption"
    "homologou"
    "communism"
    "activate"
    "angulariti"
    "homologous"
    "effective"
    "bowdlerize"]))

(let [rules-a [[(comp (ends-with "e") (m > 1)) (add "")]
               [(comp (ends-with "e") not-*o (m = 1)) (add "")]]
      rules-b [[(comp (ends-with "l") double-consonant? (m > 1)) (add "")]]]
  (defn step-5
    [word]
    (-> word
        (process rules-a)
        second
        (process rules-b)
        second)))

(comment
  (def word "probate")
  (step-5 "probate")
  (map step-5
       ["probate"
        "rate"
        "cease"
        "controll"
        "roll"]))

(defn stem
  [word]
  (-> word
      str/trim
      str/lower-case
      step-1
      step-2
      step-3
      step-4
      step-5))

(comment
  (stem "Horsing"))

(def stop-words
  #{"a" "about" "above" "after" "again" "against" "all" "am" "an" "and" "any" "are" "aren't" "as" "at"
    "be" "because" "been" "before" "being" "below" "between" "both" "but" "by"
    "can" "can't" "cannot" "could" "couldn't"
    "did" "didn't" "do" "does" "doesn't" "doing" "don't" "down" "during"
    "each" "few" "for" "from" "further"
    "had" "hadn't" "has" "hasn't" "have" "haven't" "having" "he" "he'd" "he'll" "he's" "her" "here" "here's" "hers" "herself" "him" "himself" "his" "how" "how's"
    "i" "i'd" "i'll" "i'm" "i've" "if" "in" "into" "is" "isn't" "it" "it's" "its" "itself"
    "let's"
    "me" "more" "most" "mustn't" "my" "myself"
    "no" "nor" "not" "now"
    "of" "off" "on" "once" "only" "or" "other" "ought" "our" "ours" "ourselves" "out" "over" "own"
    "same" "she" "she'd" "she'll" "she's" "should" "shouldn't" "so" "some" "such"
    "than" "that" "that's" "the" "their" "theirs" "them" "themselves" "then" "there" "there's" "these" "they" "they'd" "they'll" "they're" "they've" "this" "those" "through" "to" "too"
    "under" "until" "up"
    "very"
    "was" "wasn't" "we" "we'd" "we'll" "we're" "we've" "were" "weren't" "what" "what's" "when" "when's" "where" "where's" "which" "while" "who" "who's" "whom" "why" "why's" "with" "won't" "would" "wouldn't"
    "you" "you'd" "you'll" "you're" "you've" "your" "yours" "yourself" "yourselves"

    ;; Extra Filler Words
    "uh" "hmm" "uhh" "umm" "like" "you know" "okay" "ok"

    ;; Single letters (common non-informative words)

    ;; Conjunctions
    "yet" ""

    ;; Negatives (if you want to filter them out)
    "never" "none" "nobody"})
