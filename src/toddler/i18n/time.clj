(ns toddler.i18n.time
  (:require
   [toddler.i18n]))

(defmacro ^:no-doc add-symbols
  "EXPERIMENTAL: Macro will change toddler.i18n.time/*symbols* dynamic variable
  and add symbols for listed locales.
  
  If you wan't to add all locales use toddler.i18n/locales
  vector"
  [locales]
  (let [locales (if (sequential? locales) locales [locales])]
    `(set! toddler.i18n.time/*symbols*
           (merge toddler.i18n.time/*symbols*
                  ~(into {}
                         (map (fn [locale]
                                (let [locale-name (name locale)
                                      symbol-name (symbol (str "goog.i18n.DateTimeSymbols_" locale-name))]
                                  [(keyword locale) symbol-name]))
                              locales))))))

(defmacro ^:no-doc init-all-symbols
  "EXPERIMENTAL: Macro will change toddler.i18n.time/*symbols* dynamic variable
  and add symbols for listed locales.
  
  If you wan't to add all locales use toddler.i18n/locales
  vector"
  []
  #_(set! toddler.i18n.time/*symbols*
          (merge toddler.i18n.time/*symbols*
                 ~(into {}
                        (map (fn [locale]
                               (let [locale-name (name locale)
                                     symbol-name (symbol (str "goog.i18n.DateTimeSymbols_" locale-name))]
                                 [(keyword locale) symbol-name]))
                             toddler.i18n/locales)))))

(comment
  (macroexpand-1 '(add-locales [:en :fr :hr])))
