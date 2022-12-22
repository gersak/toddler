(ns toddler.i18n.default
  (:require
    toddler.i18n.number
    toddler.i18n.time
    [toddler.i18n.keywords :refer [add-translations]]))


(.log js/console "Loaded toddler.i18n.default")


(add-translations
  (merge
    #:time.before {:hr "prije"
                   :default "before"}
    #:time.after {:hr "poslije"
                  :default "after"}))
