(ns toddler.i18n.time
  "Include this namespace in you codebase to support
  date formating through `toddler.i18n/translate`"
  (:require
   goog.object
   [clojure.set]
   [toddler.i18n :as i18n]
   [goog.i18n.DateTimeFormat]
   [goog.i18n.DateTimeSymbols]))

; (def ^:dynamic *symbols* {:en goog.i18n.DateTimeSymbols_en})
(def ^{:dynamic true
       :doc "Decision is to provide all formatters out of the box.
            When advanced compilation is finished it take about 100kb
            in generated JS file.
            
            BUT! When gziped, difference is just 15kb, so..."}
  *symbols*
  {:fr goog.i18n.DateTimeSymbols_fr
   :en_US goog.i18n.DateTimeSymbols_en_US
   :de_AT goog.i18n.DateTimeSymbols_de_AT
   :gu goog.i18n.DateTimeSymbols_gu
   :ja goog.i18n.DateTimeSymbols_ja
   :en_IE goog.i18n.DateTimeSymbols_en_IE
   :az goog.i18n.DateTimeSymbols_az
   :ka goog.i18n.DateTimeSymbols_ka
   :el goog.i18n.DateTimeSymbols_el
   :hr goog.i18n.DateTimeSymbols_hr
   :es goog.i18n.DateTimeSymbols_es
   :pt goog.i18n.DateTimeSymbols_pt
   :en_CA goog.i18n.DateTimeSymbols_en_CA
   :ne_u_nu_latn goog.i18n.DateTimeSymbols_ne_u_nu_latn
   :fi goog.i18n.DateTimeSymbols_fi
   :ga goog.i18n.DateTimeSymbols_ga
   :ar_EG_u_nu_la goog.i18n.DateTimeSymbols_ar_EG_u_nu_la
   :tl goog.i18n.DateTimeSymbols_tl
   :is goog.i18n.DateTimeSymbols_is
   :be goog.i18n.DateTimeSymbols_be
   :es_MX goog.i18n.DateTimeSymbols_es_MX
   :en_AU goog.i18n.DateTimeSymbols_en_AU
   :pt_PT goog.i18n.DateTimeSymbols_pt_PT
   :mo goog.i18n.DateTimeSymbols_mo
   :ln goog.i18n.DateTimeSymbols_ln
   :ca goog.i18n.DateTimeSymbols_ca
   :ro goog.i18n.DateTimeSymbols_ro
   :sl goog.i18n.DateTimeSymbols_sl
   :or goog.i18n.DateTimeSymbols_or
   :th goog.i18n.DateTimeSymbols_th
   :es_ES goog.i18n.DateTimeSymbols_es_ES
   :tr goog.i18n.DateTimeSymbols_tr
   :ur goog.i18n.DateTimeSymbols_ur
   :et goog.i18n.DateTimeSymbols_et
   :en_ZA goog.i18n.DateTimeSymbols_en_ZA
   :es_419 goog.i18n.DateTimeSymbols_es_419
   :cs goog.i18n.DateTimeSymbols_cs
   :hu goog.i18n.DateTimeSymbols_hu
   :es_US goog.i18n.DateTimeSymbols_es_US
   :zh_TW goog.i18n.DateTimeSymbols_zh_TW
   :mr goog.i18n.DateTimeSymbols_mr
   :en_IN goog.i18n.DateTimeSymbols_en_IN
   :en goog.i18n.DateTimeSymbols_en
   :uz goog.i18n.DateTimeSymbols_uz
   :fr_CA goog.i18n.DateTimeSymbols_fr_CA
   :kk goog.i18n.DateTimeSymbols_kk
   :ko goog.i18n.DateTimeSymbols_ko
   :kn goog.i18n.DateTimeSymbols_kn
   :te goog.i18n.DateTimeSymbols_te
   :af goog.i18n.DateTimeSymbols_af
   :pl goog.i18n.DateTimeSymbols_pl
   :lo goog.i18n.DateTimeSymbols_lo
   :am goog.i18n.DateTimeSymbols_am
   :it goog.i18n.DateTimeSymbols_it
   :ar_DZ goog.i18n.DateTimeSymbols_ar_DZ
   :ml goog.i18n.DateTimeSymbols_ml
   :haw goog.i18n.DateTimeSymbols_haw
   :mk goog.i18n.DateTimeSymbols_mk
   :nl goog.i18n.DateTimeSymbols_nl
   :ar_EG goog.i18n.DateTimeSymbols_ar_EG
   :zh goog.i18n.DateTimeSymbols_zh
   :bn goog.i18n.DateTimeSymbols_bn
   :pa goog.i18n.DateTimeSymbols_pa
   :de_CH goog.i18n.DateTimeSymbols_de_CH
   :sr_Latn goog.i18n.DateTimeSymbols_sr_Latn
   :mn goog.i18n.DateTimeSymbols_mn
   :sk goog.i18n.DateTimeSymbols_sk
   :iw goog.i18n.DateTimeSymbols_iw
   :de goog.i18n.DateTimeSymbols_de
   :id goog.i18n.DateTimeSymbols_id
   :cy goog.i18n.DateTimeSymbols_cy
   :ar goog.i18n.DateTimeSymbols_ar
   :hy goog.i18n.DateTimeSymbols_hy
   :uk goog.i18n.DateTimeSymbols_uk
   :nb goog.i18n.DateTimeSymbols_nb
   :ta goog.i18n.DateTimeSymbols_ta
   :ru goog.i18n.DateTimeSymbols_ru
   :gl goog.i18n.DateTimeSymbols_gl
   :bs goog.i18n.DateTimeSymbols_bs
   :km goog.i18n.DateTimeSymbols_km
   :br goog.i18n.DateTimeSymbols_br
   :my_u_nu_latn goog.i18n.DateTimeSymbols_my_u_nu_latn
   :pt_BR goog.i18n.DateTimeSymbols_pt_BR
   :he goog.i18n.DateTimeSymbols_he
   :fil goog.i18n.DateTimeSymbols_fil
   :sv goog.i18n.DateTimeSymbols_sv
   :da goog.i18n.DateTimeSymbols_da
   :zu goog.i18n.DateTimeSymbols_zu
   :no_NO goog.i18n.DateTimeSymbols_no_NO
   :sh goog.i18n.DateTimeSymbols_sh
   :sw goog.i18n.DateTimeSymbols_sw
   :ne goog.i18n.DateTimeSymbols_ne
   :bn_u_nu_latn goog.i18n.DateTimeSymbols_bn_u_nu_latn
   :zh_CN goog.i18n.DateTimeSymbols_zh_CN
   :u_nu_latn goog.i18n.DateTimeSymbols_u_nu_latn
   :gsw goog.i18n.DateTimeSymbols_gsw
   :my goog.i18n.DateTimeSymbols_my
   :bg goog.i18n.DateTimeSymbols_bg
   :lt goog.i18n.DateTimeSymbols_lt
   :ms goog.i18n.DateTimeSymbols_ms
   :zh_HK goog.i18n.DateTimeSymbols_zh_HK
   :chr goog.i18n.DateTimeSymbols_chr
   :en_SG goog.i18n.DateTimeSymbols_en_SG
   :lv goog.i18n.DateTimeSymbols_lv
   :mt goog.i18n.DateTimeSymbols_mt
   :eu goog.i18n.DateTimeSymbols_eu
   :sr goog.i18n.DateTimeSymbols_sr
   :si goog.i18n.DateTimeSymbols_si
   :hi goog.i18n.DateTimeSymbols_hi
   :en_GB goog.i18n.DateTimeSymbols_en_GB
   :vi goog.i18n.DateTimeSymbols_vi
   :no goog.i18n.DateTimeSymbols_no
   :sq goog.i18n.DateTimeSymbols_sq
   :mr_u_nu_latn goog.i18n.DateTimeSymbols_mr_u_nu_latn
   :in goog.i18n.DateTimeSymbols_in
   :fa goog.i18n.DateTimeSymbols_fa
   :fa_u_nu_latn goog.i18n.DateTimeSymbols_fa_u_nu_latn
   :ky goog.i18n.DateTimeSymbols_ky})

(defn ^:no-doc get-date-symbols
  "Supported localizations"
  [locale]
  (get *symbols* locale goog.i18n.DateTimeSymbols_en))

(def ^:no-doc date-formatter
  (memoize
   (fn
     ([locale] (date-formatter locale :datetime))
     ([locale type]
      (let [target (.indexOf
                    [:full-date
                     :long-date
                     :medium-date
                     :date
                     :full-time
                     :long-time
                     :medium-time
                     :time
                     :full-datetime
                     :long-datetime
                     :medium-datetime
                     :datetime
                     :calendar]
                    type)
            pattern-idx (if (neg? target) 10 target)
            symbols (get-date-symbols locale)
            formatter ^js (goog.i18n.DateTimeFormat. pattern-idx symbols)]
        formatter)))))

(extend-protocol toddler.i18n/Translator
  js/Date
  (translate
    ([data]
     (i18n/translate data i18n/*locale*))
    ([data locale]
     (assert (keyword? locale) "Locale isn't keyword")
     (let [formatter ^js (date-formatter locale)]
       (.format formatter data)))
    ([data locale option]
     (assert (keyword? locale) "Locale isn't keyword")
     (let [formatter ^js (date-formatter locale option)]
       (.format formatter data)))))

(comment
  (time (def hr (date-formatter :hr)))
  (.format hr (js/Date.))
  (i18n/translate (js/Date.) :hr :calendar)
  (i18n/translate (js/Date.) :en_US :medium-datetime)
  (i18n/translate (js/Date.) :hr)
  (i18n/translate (js/Date.) :medium-datetime)
  (i18n/translate (js/Date.) :fr :full-datetime)
  (i18n/translate (js/Date.) :de)
  (i18n/translate (js/Date.)))
