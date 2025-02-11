(ns toddler.i18n.number)

(defmacro add-symbols
  "Macro will change toddler.i18n.number/*symbols* dynamic variable
  and add symbols for listed locales.
  
  If you wan't to add all locales use toddler.i18n/locales
  vector"
  [locales]
  (let [locales (if (sequential? locales) locales [locales])]
    `(set! toddler.i18n.number/*symbols*
           (merge toddler.i18n.number/*symbols*
                  ~(into {}
                         (map (fn [locale]
                                (let [locale-name (name locale)
                                      symbol-name (symbol (str "goog.i18n.NumberFormatSymbols_" locale-name))]
                                  [(keyword locale) symbol-name]))
                              locales))))))


