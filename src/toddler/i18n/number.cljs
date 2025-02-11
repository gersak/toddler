(ns toddler.i18n.number
  (:require-macros [toddler.i18n.number :refer [add-symbols init-all-symbols]])
  (:require
   goog.object
   [clojure.string]
   [clojure.set]
   [toddler.i18n :as i18n]
   [goog.i18n.NumberFormat]
   [goog.i18n.NumberFormatSymbols]))

;; DEPRECATED
#_{:af            goog.i18n.NumberFormatSymbols_af
   :am            goog.i18n.NumberFormatSymbols_am
   :ar            goog.i18n.NumberFormatSymbols_ar
   :ar_DZ         goog.i18n.NumberFormatSymbols_ar_DZ
   :ar_EG         goog.i18n.NumberFormatSymbols_ar_EG
   :ar_EG_u_nu_la goog.i18n.NumberFormatSymbols_ar_EG_u_nu_latn
   :az            goog.i18n.NumberFormatSymbols_az
   :be            goog.i18n.NumberFormatSymbols_be
   :bg            goog.i18n.NumberFormatSymbols_bg
   :bn            goog.i18n.NumberFormatSymbols_bn
   :bn_u_nu_latn  goog.i18n.NumberFormatSymbols_bn_u_nu_latn
   :br            goog.i18n.NumberFormatSymbols_br
   :bs            goog.i18n.NumberFormatSymbols_bs
   :ca            goog.i18n.NumberFormatSymbols_ca
   :chr           goog.i18n.NumberFormatSymbols_chr
   :cs            goog.i18n.NumberFormatSymbols_cs
   :cy            goog.i18n.NumberFormatSymbols_cy
   :da            goog.i18n.NumberFormatSymbols_da
   :de            goog.i18n.NumberFormatSymbols_de
   :de_AT         goog.i18n.NumberFormatSymbols_de_AT
   :de_CH         goog.i18n.NumberFormatSymbols_de_CH
   :el            goog.i18n.NumberFormatSymbols_el

   :en_AU         goog.i18n.NumberFormatSymbols_en_AU
   :en_CA         goog.i18n.NumberFormatSymbols_en_CA
   :en_GB         goog.i18n.NumberFormatSymbols_en_GB
   :en_IE         goog.i18n.NumberFormatSymbols_en_IE
   :en_IN         goog.i18n.NumberFormatSymbols_en_IN
   :en_SG         goog.i18n.NumberFormatSymbols_en_SG
   :en_US         goog.i18n.NumberFormatSymbols_en_US
   :en_ZA         goog.i18n.NumberFormatSymbols_en_ZA
   :es            goog.i18n.NumberFormatSymbols_es
   :es_419        goog.i18n.NumberFormatSymbols_es_419
   :es_ES         goog.i18n.NumberFormatSymbols_es_ES
   :es_MX         goog.i18n.NumberFormatSymbols_es_MX
   :es_US         goog.i18n.NumberFormatSymbols_es_US
   :et            goog.i18n.NumberFormatSymbols_et
   :eu            goog.i18n.NumberFormatSymbols_eu
   :fa            goog.i18n.NumberFormatSymbols_fa
   :fa_u_nu_latn  goog.i18n.NumberFormatSymbols_fa_u_nu_latn
   :fi            goog.i18n.NumberFormatSymbols_fi
   :fil           goog.i18n.NumberFormatSymbols_fil
   :fr            goog.i18n.NumberFormatSymbols_fr
   :fr_CA         goog.i18n.NumberFormatSymbols_fr_CA
   :ga            goog.i18n.NumberFormatSymbols_ga
   :gl            goog.i18n.NumberFormatSymbols_gl
   :gsw           goog.i18n.NumberFormatSymbols_gsw
   :gu            goog.i18n.NumberFormatSymbols_gu
   :haw           goog.i18n.NumberFormatSymbols_haw
   :he            goog.i18n.NumberFormatSymbols_he
   :hi            goog.i18n.NumberFormatSymbols_hi
   :hr            goog.i18n.NumberFormatSymbols_hr
   :hu            goog.i18n.NumberFormatSymbols_hu
   :hy            goog.i18n.NumberFormatSymbols_hy
   :id            goog.i18n.NumberFormatSymbols_id
   :in            goog.i18n.NumberFormatSymbols_in
   :is            goog.i18n.NumberFormatSymbols_is
   :it            goog.i18n.NumberFormatSymbols_it
   :iw            goog.i18n.NumberFormatSymbols_iw
   :ja            goog.i18n.NumberFormatSymbols_ja
   :ka            goog.i18n.NumberFormatSymbols_ka
   :kk            goog.i18n.NumberFormatSymbols_kk
   :km            goog.i18n.NumberFormatSymbols_km
   :kn            goog.i18n.NumberFormatSymbols_kn
   :ko            goog.i18n.NumberFormatSymbols_ko
   :ky            goog.i18n.NumberFormatSymbols_ky
   :ln            goog.i18n.NumberFormatSymbols_ln
   :lo            goog.i18n.NumberFormatSymbols_lo
   :lt            goog.i18n.NumberFormatSymbols_lt
   :lv            goog.i18n.NumberFormatSymbols_lv
   :mk            goog.i18n.NumberFormatSymbols_mk
   :ml            goog.i18n.NumberFormatSymbols_ml
   :mn            goog.i18n.NumberFormatSymbols_mn
   :mo            goog.i18n.NumberFormatSymbols_mo
   :mr            goog.i18n.NumberFormatSymbols_mr
   :mr_u_nu_latn  goog.i18n.NumberFormatSymbols_mr_u_nu_latn
   :ms            goog.i18n.NumberFormatSymbols_ms
   :mt            goog.i18n.NumberFormatSymbols_mt
   :my            goog.i18n.NumberFormatSymbols_my
   :my_u_nu_latn  goog.i18n.NumberFormatSymbols_my_u_nu_latn
   :nb            goog.i18n.NumberFormatSymbols_nb
   :ne            goog.i18n.NumberFormatSymbols_ne
   :ne_u_nu_latn  goog.i18n.NumberFormatSymbols_ne_u_nu_latn
   :nl            goog.i18n.NumberFormatSymbols_nl
   :no            goog.i18n.NumberFormatSymbols_no
   :no_NO         goog.i18n.NumberFormatSymbols_no_NO
   :or            goog.i18n.NumberFormatSymbols_or
   :pa            goog.i18n.NumberFormatSymbols_pa
   :pl            goog.i18n.NumberFormatSymbols_pl
   :pt            goog.i18n.NumberFormatSymbols_pt
   :pt_BR         goog.i18n.NumberFormatSymbols_pt_BR
   :pt_PT         goog.i18n.NumberFormatSymbols_pt_PT
   :ro            goog.i18n.NumberFormatSymbols_ro
   :ru            goog.i18n.NumberFormatSymbols_ru
   :sh            goog.i18n.NumberFormatSymbols_sh
   :si            goog.i18n.NumberFormatSymbols_si
   :sk            goog.i18n.NumberFormatSymbols_sk
   :sl            goog.i18n.NumberFormatSymbols_sl
   :sq            goog.i18n.NumberFormatSymbols_sq
   :sr            goog.i18n.NumberFormatSymbols_sr
   :sr_Latn       goog.i18n.NumberFormatSymbols_sr_Latn
   :sv            goog.i18n.NumberFormatSymbols_sv
   :sw            goog.i18n.NumberFormatSymbols_sw
   :ta            goog.i18n.NumberFormatSymbols_ta
   :te            goog.i18n.NumberFormatSymbols_te
   :th            goog.i18n.NumberFormatSymbols_th
   :tl            goog.i18n.NumberFormatSymbols_tl
   :tr            goog.i18n.NumberFormatSymbols_tr
   :u_nu_latn     goog.i18n.NumberFormatSymbols_u_nu_latn
   :uk            goog.i18n.NumberFormatSymbols_uk
   :ur            goog.i18n.NumberFormatSymbols_ur
   :uz            goog.i18n.NumberFormatSymbols_uz
   :vi            goog.i18n.NumberFormatSymbols_vi
   :zh            goog.i18n.NumberFormatSymbols_zh
   :zh_CN         goog.i18n.NumberFormatSymbols_zh_CN
   :zh_HK         goog.i18n.NumberFormatSymbols_zh_HK
   :zh_TW         goog.i18n.NumberFormatSymbols_zh_TW
   :zu            goog.i18n.NumberFormatSymbols_zu}

(defonce ^:dynamic *symbols* {:en goog.i18n.NumberFormatSymbols_en})

(def currency-map
  (reduce
   (fn [cm ^js s]
     (assoc cm
       (.-DEF_CURRENCY_CODE s)
       (.-CURRENCY_PATTERN s)))
   nil
   (vals *symbols*)))

(def currency-formatters
  (reduce-kv
   (fn [cf currency pattern]
     (assoc cf currency (goog.i18n.NumberFormat. pattern currency)))
   nil
   currency-map))

(defn number-formatter [locale]
  (let [^js symbols (get *symbols* locale (:en *symbols*))
        number-formatter-pattern
        (goog.i18n.NumberFormat.
         (.-DECIMAL_PATTERN symbols)
         nil
         nil
         symbols)]
    (fn format-number [x] (.format ^js number-formatter-pattern x))))

(defn format-currency [currency value]
  (when-let [^js f (currency-formatters currency)]
    (.format f value)))

(.log js/console "i18n numbers formaters generated!")

(extend-protocol toddler.i18n/Translator
  number
  (translate
    ([data]
     (let [formatter (number-formatter i18n/*locale*)]
       (formatter data)))
    ([data format]
     (let [is-currency (string? format)
           is-locale (keyword? format)]
       (try
         (cond
           is-currency (.format (currency-formatters format) data)
           is-locale ((number-formatter format) data)
           :else (str "Unsupported input: " format))
         (catch js/Error e (str "Invalid currency or locale: " format)))))))

(comment
  (i18n/translate 2500 :en_US)
  (i18n/translate 25.5 "HRK")
  (i18n/translate 1000000 :hr)
  (i18n/translate 69999.99 "EUR")
  (i18n/translate 12345 :kk)
  (i18n/translate 12325.39 "JPY"))

(comment
  (keys symbols)
  (println #:toddler.i18n {:a 100 :b 200})
  (keys goog.i18n.NumberFormatSymbols)
  (.format (currency-formatters "EGP") 1092)
  (.format (currency-formatters "USD") 1092)
  (.format (currency-formatters "HRK") 1092)
  ((number-formatter :ar_EG) 1791)
  goog.i18n.NumberFormatSymbols_ja)
