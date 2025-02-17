(ns toddler.i18n.uuid
  "This namespace is used for translating words or paragraphs
  by replacing UUID that is supplied to `toddler.i18n/translate`
  with value that is mapped to UUID and locale"
  (:require
   [toddler.i18n :as i18n]
   [toddler.util :refer [deep-merge]]))

(defonce ^{:doc "Atom that contains translation mapping"} translations (atom nil))

(defn add-translations
  "Function that will update [[translations]] and merge
  input `mapping` with deep merge with current value of
  [[translations]].
  
  Translations should be in form:
  ```
  #uuid \"a1b2c3d4-e5f6-7890-1234-567890abcdef\" {:default \"Specific word\"
                                                  :de \"Bestimmtes Wort\"
                                                  :fr \"Mot spécifique\"
                                                  :es \"Palabra específica\"}
  ```"
  [mapping]
  (swap! translations deep-merge mapping))

(defn add-locale
  "Same as [[add-translations]] only format is different
  
  ```
  #:default {#uuid \"a1b2c3d4-e5f6-7890-1234-567890abcdef\" \"Specific word\"
             #uuid \"f8a7b6c5-d4e3-4210-9876-543210fedcba\" \"Other word\"
             #uuid \"e091f2d3-c4b5-4a89-0123-456789abcdef\" \"Job\"
             #uuid \"9786a5b4-c3d2-410f-e987-6543210fedcb\" \"Shit\"}
  ```"
  [mapping]
  (swap! translations deep-merge
         (reduce-kv
          (fn [r k v]
            (assoc r (keyword (name k) (namespace k)) v))
          nil
          mapping)))

(defn remove-translations
  "Will remove translations for uuid and locales"
  [uuid locales]
  (swap! translations update uuid
         (fn [translations]
           (reduce-kv
            (fn [r k _]
              (cond-> r
                (contains? locales k) (dissoc k)))
            translations
            translations))))

(extend-protocol toddler.i18n/Translator
  cljs.core.UUID
  (translate
    ([this]
     (get-in @translations [this :default]))
    ([this locale]
     (assert (keyword? locale) (str "Locale shoudld be keyword: " (pr-str locale)))
     (if-let [w (get-in @translations [this locale])]
       w
       (i18n/translate this)))))

(comment
  (def component #uuid "52bd2dec-7024-4062-baf1-6c3fc7b87492")
  (add-translations
   {component {:default "Data" :hr "Podaci"}})
  (i18n/translate component)
  (i18n/translate component :hr)
  (inc (mod 1 7))
  (mod 8 8)
  (println
   #:toddler.test {:a 100 :b 200})
  (namespace ::fjqoi)
  (deref translations)
  (i18n/locale :hr :months/standalone)
  (i18n/locale :fr :months)
  (i18n/locale :hr :weekdays/short)
  (i18n/locale :hr :weekends)
  (i18n/locale :hr :weekdays/first)
  (i18n/locale :en_US :weekdays/first)
  (i18n/locale :hr :quarters)
  (add-translations
   #:toddler.dog.test {:hr "pas"
                       :en "dog"
                       :de "hund"})
  (get @translations (keyword :toddler.dog.test :hr))
  (time (i18n/translate :toddler.dog.test/hr))
  (time (i18n/translate :toddler.dog.test :hr))
  (i18n/translate :toddler.dog.test :en)
  (i18n/translate :toddler.dog.test :de))
