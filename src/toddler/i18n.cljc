(ns toddler.i18n)

(def ^{:doc "Vector of all locales"}
  locales
  [:fr
   :en_US
   :de_AT
   :gu
   :ja
   :en_IE
   :az
   :ka
   :el
   :hr
   :es
   :pt
   :en_CA
   :ne_u_nu_latn
   :fi
   :ga
   :ar_EG_u_nu_la
   :tl
   :is
   :be
   :es_MX
   :en_AU
   :pt_PT
   :mo
   :ln
   :ca
   :ro
   :sl
   :or
   :th
   :es_ES
   :tr
   :ur
   :et
   :en_ZA
   :es_419
   :cs
   :hu
   :es_US
   :zh_TW
   :mr
   :en_IN
   :uz
   :fr_CA
   :kk
   :ko
   :kn
   :te
   :af
   :pl
   :lo
   :am
   :it
   :ar_DZ
   :ml
   :haw
   :mk
   :nl
   :ar_EG
   :zh
   :bn
   :pa
   :de_CH
   :sr_Latn
   :mn
   :sk
   :iw
   :de
   :id
   :cy
   :ar
   :hy
   :uk
   :nb
   :ta
   :ru
   :gl
   :bs
   :km
   :br
   :my_u_nu_latn
   :pt_BR
   :he
   :fil
   :sv
   :da
   :zu
   :no_NO
   :sh
   :sw
   :ne
   :bn_u_nu_latn
   :zh_CN
   :u_nu_latn
   :gsw
   :my
   :bg
   :lt
   :ms
   :zh_HK
   :chr
   :en_SG
   :lv
   :mt
   :eu
   :sr
   :si
   :hi
   :en_GB
   :vi
   :no
   :sq
   :mr_u_nu_latn
   :in
   :fa
   :fa_u_nu_latn
   :ky])

(def ^:dynamic *locale*
  #?(:cljs
     (let [browser-locales (.-languages js/navigator)
           {[general] false
            [specific] true} (group-by #(.includes % "-") browser-locales)]
       (if-some [locale (or general specific)]
         (keyword locale)
         :en))))

(defprotocol Translator
  (translate
    [this]
    [this locale]
    [this locale options]
    "Translates input data by using additional opitons"))

(defprotocol Locale
  (locale [this key] "Returns locale definition for given key"))

#?(:cljs (extend-type string
           Translator
           (translate
             ([this] this)
             ([this _] this)
             ([this _ _] this))))

(extend-type nil
  Translator
  (translate
    ([_] nil)
    ([_ _] nil)
    ([_ _ _] nil)))

(comment
  (type "")
  (translate (js/Date.) :hr :medium-datetime))
