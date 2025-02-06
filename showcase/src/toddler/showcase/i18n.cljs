(ns toddler.showcase.i18n
  (:require
   [helix.core :refer [defnc $ <> provider]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.ui :as ui :refer [!]]
   [toddler.app :as app]
   [toddler.router :as router]
   [toddler.layout :as layout]
   [toddler.i18n :as i18n]
   [toddler.i18n.keyword
    :refer [add-translations
            add-locale]]
   [toddler.md.lazy :as md]
   [toddler.showcase.css :as css]
   [toddler.core :as toddler]))

(add-locale
 (merge
  #:default {:first-name "First Name"
             :last-name "Last Name"
             :address "Address"
             :country "Country"
             :region "Region"
             :nationality "Nationality"
             :religion "Religion"
             :gender "Gender"}
  #:de {:first-name "Vorname"
        :last-name "Nachname"
        :address "Adresse"
        :country "Land"
        :region "Region"
        :nationality "Staatsangehörigkeit"
        :religion "Religion"
        :gender "Geschlecht"}
  #:fr {:first-name "Prénom"
        :last-name "Nom de famille"
        :address "Adresse"
        :country "Pays"
        :region "Région"
        :nationality "Nationalité"
        :religion "Religion"
        :gender "Genre"}
  #:es {:first-name "Nombre"
        :last-name "Apellido"
        :address "Dirección"
        :country "País"
        :region "Región"
        :nationality "Nacionalidad"
        :religion "Religión"
        :gender "Género"}
  #:ja {:first-name "名前"
        :last-name "苗字"
        :address "住所"
        :country "国"
        :region "地域"
        :nationality "国籍"
        :religion "宗教"
        :gender "性別"}
  #:zh_CN {:first-name "名字"
           :last-name "姓"
           :address "地址"
           :country "国家"
           :region "地区"
           :nationality "国籍"
           :religion "宗教"
           :gender "性别"}))

(add-translations
 (merge
  #:showcase.i18n {:default "i18n"}
  #:date {:default "Date"
          :en "Date"
          :en_US "Date"
          :en_IN "Date"
          :de "Datum"
          :fr "Date"
          :es "Fecha"
          :ja "日付"
          :zh_CN "日期"}
  #:account.balance {:default "Account Balance"
                     :en "Account Balance"
                     :en_US "Account Balance"
                     :en_IN "Account Balance"
                     :de "Kontostand"
                     :fr "Solde du compte"
                     :es "Saldo de cuenta"
                     :ja "口座残高"
                     :zh_CN "账户余额"}
  #:weather {:default "Weather"
             :en "Weather"
             :en_US "Weather"
             :en_IN "Weather"
             :de "Wetter"
             :fr "Météo"
             :es "Clima"
             :ja "天気"
             :zh_CN "天气"}
  #:weather.report {:default "Today will be %s day with %02.02f%% rain possibility"
                    :en "Today will be %s day with %02.02f%% rain possibility"
                    :en_US "Today will be %s day with %02.02f%% rain possibility"
                    :en_IN "Today will be %s day with %02.02f%% rain possibility"
                    :de "Heute wird ein %s Tag mit %02.02f%% Regenwahrscheinlichkeit"
                    :fr "Aujourd'hui sera une journée %s avec %02.02f%% de chance de pluie"
                    :es "Hoy será un día %s con %02.02f%% de probabilidad de lluvia"
                    :ja "今日は%s日で、雨の可能性は%02.02f%%です"
                    :zh_CN "今天将是一个%s的日子，有%02.02f%%的降雨可能"}
  #:weather.sunny {:default "sunny"
                   :en "sunny"
                   :en_US "sunny"
                   :en_IN "sunny"
                   :de "sonnig"
                   :fr "ensoleillé"
                   :es "soleado"
                   :ja "晴れ"
                   :zh_CN "晴天"}
  #:weather.cloudy {:default "cloudy"
                    :en "cloudy"
                    :en_US "cloudy"
                    :en_IN "cloudy"
                    :de "bewölkt"
                    :fr "nuageux"
                    :es "nublado"
                    :ja "曇り"
                    :zh_CN "多云"}))

(defn translate-example []
  (let [translate (toddler/use-translate)
        translatef (toddler/use-translatef)
        locale (toddler/use-current-locale)
        local-currency {:en "GBP"
                        :en_US "USD"
                        :de "EUR"
                        :fr "EUR"
                        :es "EUR"
                        :ja "JPY"
                        :en_IN "INR"
                        :zh_CN "CNY"}
        $field (css :font-bold)]
    (! :row
       {:className css/$default
        :align :center}
       (d/div
        {:class ["table-container" (css ["& td" :px-2])]}
        (d/table
         (d/tbody
          (d/tr
           (d/td {:className $field} (translate :date))
           (d/td (translate (js/Date.) :full-datetime)))
             ;;
          (d/tr
           (d/td {:className $field} (translate :account.balance))
           (d/td (translate (rand 10000) (get local-currency locale))))
          (d/tr
           (d/td {:className $field} (translate :weather))
           (d/td (translatef :weather.report
                             (translate (rand-nth [:weather.sunny :weather.cloudy]))
                             (rand 100))))))))))

(defnc App []
  (let [[locale set-locale!] (hooks/use-state :en)]
    (provider
     {:context app/locale
      :value locale}
     (<>
      (! :row
         (! :field/dropdown
            {:name "Locale"
             :value locale
             :on-change set-locale!
             :options [:en :en_US :en_IN :en :de :fr :es :ja :zh_CN]
             :search-fn #(i18n/translate :locale %)}))
      ($ translate-example)))))

(defnc i18n-example []
  (let [locale (toddler.core/use-current-locale)]
    (d/div
     {:className (css :p-4 :mt-4 :font-semibold :bg-yellow-200 :rounded-xl :text-black)}
     (d/div
      {:className "content"}
      (case locale
        :de
        (d/p "Hallo, dies ist ein Beispielkomponent. "
             "Versuchen Sie, die Sprache zu ändern, um zu sehen, wie sich dieser Satz ändert.")
        :fr
        (d/p "Bonjour, ceci est un composant d'exemple. "
             "Essayez de changer la langue pour voir comment cette phrase change.")
        :es
        (d/p "Hola, este es un componente de ejemplo. "
             "Intenta cambiar la configuración regional para ver cómo cambia esta oración.")
        :en
        (d/p "Hello, this is an example component. "
             "Try and change the locale to see how this sentence will change.")
        :ja
        (d/p "こんにちは、これは例のコンポーネントです。"
             "ロケールを変更して、この文がどのように変化するかを確認してください。")
        :zh_CN
        (d/p "你好，这是一个示例组件。"
             "尝试更改区域设置，看看这句话会如何变化。")
        (d/p "Hello, this is an example component. "
             "Try and change locale to see how this sentence will change"))))))

(defnc i18n-content
  {:wrap [(router/wrap-rendered :toddler.i18n)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        [locale set-locale!] (hooks/use-state :en)]
    (provider
     {:context app/locale
      :value locale}
     (! :simplebar
        {:style {:height height
                 :width width}
         :shadow true}
        (! :row {:align :center}
           (! :column
              {:align :center
               :className (css :items-center ["& .toddler-markdown" {:max-width "40rem"}])}
              ($ md/watch-url {:url "/i18n.md"})
              ($ toddler/portal
                 {:locator #(.getElementById js/document "component-translation-example")}
                 ($ i18n-example))
              ($ toddler/portal
                 {:locator #(.getElementById js/document "common-translation-example")}
                 (<>
                  (! :row
                     (! :field/dropdown
                        {:name "choose locale"
                         :value locale
                         :on-change set-locale!
                         :options [:en :en_US :en_IN :en :de :fr :es :ja :zh_CN]
                         :search-fn #(i18n/translate :locale %)}))
                  ($ translate-example)))))))))

(defnc i18n []
  (router/use-link
   :toddler.i18n
   [{:id ::adding-translations
     :name "Adding Translations"
     :hash "adding-translations"}
    {:id ::adding-locale
     :name "Adding Locale"
     :hash "adding-locale"}
    {:id ::hooks
     :name "Hooks"
     :hash "hooks"}])
  ($ i18n-content))
