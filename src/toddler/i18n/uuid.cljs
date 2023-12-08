(ns toddler.i18n.uuid
  (:require
   [toddler.i18n :as i18n]
   [helix.placenta.util :refer [deep-merge]]))


(defonce translations (atom nil))


(defn add-translations
  [mapping]
  (swap! translations deep-merge mapping))


(defn remove-translations
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
