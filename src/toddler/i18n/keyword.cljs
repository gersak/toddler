(ns toddler.i18n.keyword
  (:require
   [toddler.i18n :as i18n]
   [toddler.i18n.time :as t]
   [helix.placenta.util :refer [deep-merge]]))


(defonce translations (atom nil))


(defn add-translations
  [mapping]
  (swap! translations deep-merge mapping))


(defn remove-translations
  [key locales]
  (swap! translations update key
         (fn [translations]
           (reduce-kv
            (fn [r k _]
              (cond-> r
                (contains? locales k) (dissoc k)))
            translations
            translations))))


(extend-protocol toddler.i18n/Translator
  cljs.core.Keyword
  (translate
    ([this]
     (assert (qualified-keyword? this)
             "Keyword is not qualified. Either specify locale or add locale to qualified namespace")
     (get @translations this))
    ([this locale]
     (assert (not (qualified-keyword? this))
             "Keyword is qualified. Remove locale from arguments since it is already contained in qualified keyword")
     (assert (keyword? locale) "Locale shoudld be keyword")
     (if-let [w (get @translations (keyword (name this) (name locale)))]
       w
       (get @translations (keyword (name this) :default))))))


(extend-protocol toddler.i18n/Locale
  cljs.core.Keyword
  (locale [this subject]
    (let [^js symbols (t/get-date-symbols this)]
      (case subject
        :months (.-MONTHS symbols)
        :months/standalone (.-STANDALONEMONTHS symbols)
        :months/short (.-SHORTMONTHS symbols)
        :months.standalone/short (.-STANDALONESHORTMONTHS symbols)
        :eras (.-ERAS symbols)
        :era/names (.-ERANAMES symbols)
        :months/narrow (.-NARROWMONTHS symbols)
        :weekdays (.-WEEKDAYS symbols)
        :weekdays/standalone (.-STANDALONEWEEKDAYS symbols)
        :weekdays/short (.-SHORTWEEKDAYS symbols)
        :weekdays.standalone/short (.-STANDALONESHORTWEEKDAYS symbols)
        :weekdays/narrow (.-NARROWWEEKDAYS symbols)
        :weekdays.standalone/narrow (.-STANDALONENARROWWEEKDAYS symbols)
        :quarters (.-QUARTERS symbols)
        :quarters/short (.-SHORTQUARTERS symbols)
        :ampms (.-AMPMS symbols)
        :weekends (set (map inc (.-WEEKENDRANGE symbols)))
        :weekdays/first (inc (.-FIRSTDAYOFWEEK symbols))))))


(.log js/console "Loaded toddler.i18n.keywords")


(comment
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
