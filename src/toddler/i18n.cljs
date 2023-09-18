(ns toddler.i18n)


(def ^:dynamic *locale*
  (let [browser-locales (.-languages js/navigator)
        {[general] false
         [specific] true} (group-by #(.includes % "-") browser-locales)]
    (if-some [locale (or general specific)]
      (keyword locale)
      :en)))


(defprotocol Translator
  (translate
    [this]
    [this locale]
    [this locale options]
    "Translates input data by using additional opitons"))


(defprotocol Locale
  (locale [this key] "Returns locale definition for given key"))


(extend-type string
  Translator
  (translate
    ([this] this)
    ([this _] this)
    ([this _ _] this)))


(extend-type nil
  Translator
  (translate
    ([_] nil)
    ([_ _] nil)
    ([_ _ _] nil)))


(comment
  (type "")
  (translate (js/Date.) :hr :medium-datetime))
