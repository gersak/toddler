(ns toddler.i18n.common
  (:require
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 (merge
  #:open {:default "Open" :hr "Otvori" :es "Abrir" :de "Öffnen" :fr "Ouvrir"}
  #:neutral {:default "Neutral" :hr "Neutralan" :es "Neutral" :de "Neutral" :fr "Neutre"}
  #:positive {:default "Positive" :hr "Pozitivan" :es "Positivo" :de "Positiv" :fr "Positif"}
  #:negative {:default "Negative" :hr "Negativan" :es "Negativo" :de "Negativ" :fr "Négatif"}
  #:warn {:default "Warning" :hr "Upozorenje" :es "Advertencia" :de "Warnung" :fr "Avertissement"}
  #:ok {:default "OK" :hr "OK" :es "OK" :de "OK" :fr "OK"}
  #:cancel {:default "Cancel" :hr "Odbaci" :es "Cancelar" :de "Abbrechen" :fr "Annuler"}
  #:add {:default "Add" :hr "Dodaj" :es "Añadir" :de "Hinzufügen" :fr "Ajouter"}
  #:delete {:default "Delete" :hr "Izbriši" :es "Eliminar" :de "Löschen" :fr "Supprimer"}
  #:remove {:default "Remove" :hr "Ukloni" :es "Eliminar" :de "Entfernen" :fr "Supprimer"}
  #:first-name {:default "First Name" :hr "Ime" :es "Nombre" :de "Vorname" :fr "Prénom"}
  #:last-name {:default "Last Name" :hr "Prezime" :es "Apellido" :de "Nachname" :fr "Nom"}
  #:address {:default "Address" :hr "Adresa" :es "Dirección" :de "Adresse" :fr "Adresse"}
  #:post-code {:default "Post Code" :hr "Poštanski kod" :es "Código Postal" :de "Postleitzahl" :fr "Code Postal"}
  #:credit-card {:default "Credit Card" :hr "Kreditna kartica" :es "Tarjeta de Crédito" :de "Kreditkarte" :fr "Carte de Crédit"}
  #:image {:default "Image" :hr "Slika" :es "Imagen" :de "Bild" :fr "Image"}
  #:example {:default "Example" :hr "Primjer" :es "Ejemplo" :de "Beispiel" :fr "Exemple"}
  #:examples {:default "Examples" :hr "Primjeri" :es "Ejemplos" :de "Beispiele" :fr "Exemples"}
  #:result {:default "Result" :de "Ergebnis" :fr "Résultat" :es "Resultado" :hr "Rezultat"}
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
