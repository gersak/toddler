(ns toddler.i18n.number
  (:require
   [toddler.i18n]))

(defmacro add-symbols
  "Macro will change toddler.i18n.number/*symbols* dynamic variable
  and add symbols for listed locales.
  
  If you wan't to add all locales use toddler.i18n/locales
  vector"
  [locales]
  `(do
     (set! toddler.i18n.number/*symbols*
           (merge toddler.i18n.number/*symbols*
                  ~(into {}
                         (map (fn [locale]
                                (let [locale-name (name locale)
                                      symbol-name (symbol (str "goog.i18n.NumberFormatSymbols_" locale-name))]
                                  [(keyword locale) symbol-name]))
                              locales))))
     (toddler.i18n.number/refresh-currency-formatters)))

(defmacro init-all-symbols
  []
  `(do
     (set! toddler.i18n.number/*symbols*
           (merge toddler.i18n.number/*symbols*
                  ~(into {}
                         (map (fn [locale]
                                (let [locale-name (name locale)
                                      symbol-name (symbol (str "goog.i18n.NumberFormatSymbols_" locale-name))]
                                  [(keyword locale) symbol-name]))
                              toddler.i18n/locales))))
     (toddler.i18n.number/refresh-currency-formatters)))


