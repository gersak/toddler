(ns toddler.i18n.common
  (:require
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 (merge
  #:open {:default "Open"
          :hr "Otvori"}
  #:neutral {:hr "Neutralan"
             :default "Neutral"}
  #:positive {:hr "Pozitivan"
              :default "Positive"}
  #:negative {:hr "Negativan"
              :default "Negative"}
  #:warn {:hr "Upozorenje"
          :default "Warning"}
  #:ok {:hr "OK" :default "OK"}
  #:cancel {:hr "Odbaci" :default "Cancel"}
  #:locale {:default "English"
            :hr "Hrvatski"        ;; Croatian
            :de "Deutsch"         ;; German
            :fr "Français"        ;; French
            :es "Español"         ;; Spanish
            :it "Italiano"        ;; Italian
            :pt "Português"       ;; Portuguese
            :ru "Русский"         ;; Russian
            :zh "中文"            ;; Chinese
            :ja "日本語"          ;; Japanese
            :ko "한국어"          ;; Korean
            :ar "العربية"         ;; Arabic
            :nl "Nederlands"      ;; Dutch
            :sv "Svenska"         ;; Swedish
            :fi "Suomi"           ;; Finnish
            :no "Norsk"           ;; Norwegian
            :da "Dansk"           ;; Danish
            :pl "Polski"          ;; Polish
            :tr "Türkçe"          ;; Turkish
            :el "Ελληνικά"        ;; Greek
            :cs "Čeština"         ;; Czech
            :sk "Slovenčina"      ;; Slovak
            :hu "Magyar"          ;; Hungarian
            :ro "Română"          ;; Romanian
            :bg "Български"       ;; Bulgarian
            :sr "Српски"          ;; Serbian
            :uk "Українська"      ;; Ukrainian
            :he "עברית"           ;; Hebrew
            :th "ไทย"             ;; Thai
            :vi "Tiếng Việt"      ;; Vietnamese
            :sw "Kiswahili"       ;; Swahili
            }))
