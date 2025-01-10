(ns toddler.showcase.i18n
  (:require
   [helix.core :refer [defnc $ <>]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.layout :as layout]
   [toddler.i18n :refer [translate]]
   [toddler.i18n.keyword
    :refer [add-translations
            add-locale]]
   [toddler.hooks :as toddler]
   [toddler.showcase.common :refer [$info use-code-refresh]]))

(defnc i18n-example []
  (let [locale (toddler.hooks/use-current-locale)]
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
        :hr
        (d/p "Pozdrav, ovo je primjer komponente. "
             "Pokušajte promijeniti lokalne postavke da vidite kako će se ova rečenica promijeniti.")
        (d/p "hello this is example component. "
             "Try and change locale to see how this sentence will change"))))))

(add-translations
 #:i18n.showcase.example {:default i18n-example})

(defnc show-i18n-example
  []
  (let [translate (toddler.hooks/use-translate)]
    ($ (translate :i18n.showcase.example))))

(defnc i18nStory []
  (let [locale (toddler/use-current-locale)
        _translate (toddler/use-translate)]
    (use-code-refresh)
    (letfn [(translator-definition []
              (d/pre
               {:className "code"}
               (d/code
                "(defprotocol Translator
  (translate
    [this]
    [this locale]
    [this locale options]
    \"Translates input data by using additional opitons\"))")))
            (translate-code-example []
              (d/pre
               {:className "code"}
               (d/code
                "
(translate 100000.001 :en_US) ;; \"100,000.001\"
(translate 100000.001 :fr)    ;; \"100 000,001\"
(translate 100000.001 \"JPY\")  ;; \"¥100,000.00\"
(translate 100000.001 \"INR\")  ;; \"₹100,000.00\"
(translate 100000.001 \"BRL\")  ;; \"R$ 100,000.00\"

;; Default format is short-datetime
(translate (js/Date.) :en_US)                   ;; \"1/10/25, 1:43 PM\"
(translate (js/Date.) :en_US :date)             ;; \"1/10/25\"
(translate (js/Date.) :en_US :full-datetime)    ;; \"Friday, January 10, 2025 at 1:44:27 PM UTC+1\"
(translate (js/Date.) :en_US :medium-datetime)  ;; \"Jan 10, 2025, 1:45:06 PM\"
")))
            (add-translations-code []
              (d/pre
               {:className "code"}
               (d/code
                "(ns toddler.i18n.common
  (:require
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 (merge
  #:open {:default \"Open\" :hr \"Otvori\" :es \"Abrir\" :de \"Öffnen\" :fr \"Ouvrir\"}
  #:neutral {:default \"Neutral\" :hr \"Neutralan\" :es \"Neutral\" :de \"Neutral\" :fr \"Neutre\"}
  #:positive {:default \"Positive\" :hr \"Pozitivan\" :es \"Positivo\" :de \"Positiv\" :fr \"Positif\"}
  #:negative {:default \"Negative\" :hr \"Negativan\" :es \"Negativo\" :de \"Negativ\" :fr \"Négatif\"}
  #:warn {:default \"Warning\" :hr \"Upozorenje\" :es \"Advertencia\" :de \"Warnung\" :fr \"Avertissement\"}
  #:ok {:default \"OK\" :hr \"OK\" :es \"OK\" :de \"OK\" :fr \"OK\"}
  #:cancel {:default \"Cancel\" :hr \"Odbaci\" :es \"Cancelar\" :de \"Abbrechen\" :fr \"Annuler\"}
  #:add {:default \"Add\" :hr \"Dodaj\" :es \"Añadir\" :de \"Hinzufügen\" :fr \"Ajouter\"}
  #:delete {:default \"Delete\" :hr \"Izbriši\" :es \"Eliminar\" :de \"Löschen\" :fr \"Supprimer\"}
  #:remove {:default \"Remove\" :hr \"Ukloni\" :es \"Eliminar\" :de \"Entfernen\" :fr \"Supprimer\"}
  #:first-name {:default \"First Name\" :hr \"Ime\" :es \"Nombre\" :de \"Vorname\" :fr \"Prénom\"}
  #:last-name {:default \"Last Name\" :hr \"Prezime\" :es \"Apellido\" :de \"Nachname\" :fr \"Nom\"}
  #:address {:default \"Address\" :hr \"Adresa\" :es \"Dirección\" :de \"Adresse\" :fr \"Adresse\"}
  #:post-code {:default \"Post Code\" :hr \"Poštanski kod\" :es \"Código Postal\" :de \"Postleitzahl\" :fr \"Code Postal\"}
  #:credit-card {:default \"Credit Card\" :hr \"Kreditna kartica\" :es \"Tarjeta de Crédito\" :de \"Kreditkarte\" :fr \"Carte de Crédit\"}
  #:image {:default \"Image\" :hr \"Slika\" :es \"Imagen\" :de \"Bild\" :fr \"Image\"}
  #:example {:default \"Example\" :hr \"Primjer\" :es \"Ejemplo\" :de \"Beispiel\" :fr \"Exemple\"}
  #:examples {:default \"Examples\" :hr \"Primjeri\" :es \"Ejemplos\" :de \"Beispiele\" :fr \"Exemples\"}
  #:locale {:default \"English\"
            :hr \"Hrvatski\"        ;; Croatian
            :de \"Deutsch\"         ;; German
            :fr \"Français\"        ;; French
            :es \"Español\"         ;; Spanish
            :it \"Italiano\"        ;; Italian
            :pt \"Português\"       ;; Portuguese
            :ru \"Русский\"         ;; Russian
            :zh \"中文\"            ;; Chinese
            :ja \"日本語\"          ;; Japanese
            :ko \"한국어\"          ;; Korean
            :ar \"العربية\"         ;; Arabic
            :nl \"Nederlands\"      ;; Dutch
            :sv \"Svenska\"         ;; Swedish
            :fi \"Suomi\"           ;; Finnish
            :no \"Norsk\"           ;; Norwegian
            :da \"Dansk\"           ;; Danish
            :pl \"Polski\"          ;; Polish
            :tr \"Türkçe\"          ;; Turkish
            :el \"Ελληνικά\"        ;; Greek
            :cs \"Čeština\"         ;; Czech
            :sk \"Slovenčina\"      ;; Slovak
            :hu \"Magyar\"          ;; Hungarian
            :ro \"Română\"          ;; Romanian
            :bg \"Български\"       ;; Bulgarian
            :sr \"Српски\"          ;; Serbian
            :uk \"Українська\"      ;; Ukrainian
            :he \"עברית\"           ;; Hebrew
            :th \"ไทย\"             ;; Thai
            :vi \"Tiếng Việt\"      ;; Vietnamese
            :sw \"Kiswahili\"       ;; Swahili
            }))")))
            (add-component-code []
              (d/pre
               {:class "code"}
               (d/code
                "(defnc i18n-example []
  (let [locale (toddler.hooks/use-current-locale)]
    (d/div
     {:className (css :p-4 :mt-4 :font-semibold :bg-yellow-200 :rounded-xl :text-black)}
     (d/div
      {:className \"content\"}
      (case locale
        :de
        (d/p \"Hallo, dies ist ein Beispielkomponent. \"
             \"Versuchen Sie, die Sprache zu ändern, um zu sehen, wie sich dieser Satz ändert.\")
        :fr
        (d/p \"Bonjour, ceci est un composant d'exemple. \"
             \"Essayez de changer la langue pour voir comment cette phrase change.\")
        :es
        (d/p \"Hola, este es un componente de ejemplo. \"
             \"Intenta cambiar la configuración regional para ver cómo cambia esta oración.\")
        :hr
        (d/p \"Pozdrav, ovo je primjer komponente. \"
             \"Pokušajte promijeniti lokalne postavke da vidite kako će se ova rečenica promijeniti.\")
        (d/p \"hello this is example component. \"
             \"Try and change locale to see how this sentence will change\"))))))

(add-translations
 #:i18n.showcase.example {:default i18n-example})

(defnc show-i18n-example
  []
  (let [translate (toddler.hooks/use-translate)]
    ($ (translate :i18n.showcase.example))))")))]
      (d/div
       {:className $info}
       (case locale
         :de
         (<>
          (d/p "Hallo,")
          (d/p "Willkommen zur i18n-Demonstration. Im folgenden Text zeigen wir, wie "
               "man Übersetzungen hinzufügt und wie man \"use-translate\" in UI-Komponenten "
               "verwendet.")
          (d/p "Vorher schauen wir uns an, wie Toddler Übersetzungen angeht. "
               "Im Namespace " (d/b "toddler.i18n") " gibt es eine Definition für das "
               (d/b "Translator") "-Protokoll. Es ist eine einfache Definition, die "
               "basierend auf einer bestimmten Locale und optionalen Optionen \"übersetzt\"."))
         :fr
         (<>
          (d/p "Bonjour,")
          (d/p "Bienvenue à la démonstration i18n. Dans le texte suivant, nous allons présenter "
               "comment ajouter des traductions et comment utiliser \"use-translate\" dans des "
               "composants d'interface utilisateur.")
          (d/p "Avant cela, examinons comment Toddler aborde la traduction. "
               "Dans l'espace de noms " (d/b "toddler.i18n") ", il y a une définition pour le "
               (d/b "Translator") " protocole. C'est une définition simple qui "
               "« traduit » en fonction d'une certaine locale et d'options facultatives."))
         :es
         (<>
          (d/p "Hola,")
          (d/p "Bienvenido a la demostración de i18n. En el siguiente texto, presentaremos cómo "
               "agregar traducciones y cómo usar \"use-translate\" dentro de los componentes de la UI.")
          (d/p "Antes de eso, repasemos cómo Toddler aborda las traducciones. "
               "En el espacio de nombres " (d/b "toddler.i18n") " hay una definición para el "
               (d/b "Translator") " protocolo. Es una definición simple que "
               "\"traduce\" según una configuración regional específica y opciones opcionales."))
         :hr
         (<>
          (d/p "Pozdrav,")
          (d/p "Dobrodošli u i18n prikaz. U sljedećem tekstu ćemo prikazati kako "
               "dodati prijevode i kako koristiti \"use-translate\" unutar UI komponenti.")
          (d/p "Prije toga, pogledajmo kako Toddler pristupa prijevodima. "
               "U imeniku " (d/b "toddler.i18n") " nalazi se definicija za "
               (d/b "Translator") " protokol. To je jednostavna definicija koja "
               "\"prevodi\" na temelju određene lokalne postavke i opcionalnih opcija."))
         (<>
          (d/p "Hi,")
          (d/p "Welcome to i18n showcase. In following text we'll present how "
               "how to add translations and how to use-translate inside of UI "
               "components.")
          (d/p "Before that, lets go through how toddler aproaches translation."
               " In namespace " (d/b "toddler.i18n") " there is definition for "
               (d/b "Translator") " protocol. It is simple definition that "
               " \"translates\" based on some locale and optional options.")))
       (translator-definition)
       (case locale
         :de (d/p "Implementierungen dieses Protokolls existieren für:")
         :fr (d/p "Des implémentations de ce protocole existent pour :")
         :es (d/p "Existen implementaciones de este protocolo para:")
         :hr (d/p "Implementacije ovog protokola postoje za:")
         (d/p "Implementations of this protocol exist for:"))
       (d/ul
        {:className (css ["& b" :font-medium])}
        (d/li (d/b "keyword"))
        (d/li (d/b "number"))
        (d/li (d/b "Date"))
        (d/li (d/b "UUID")))
       (case locale
         :de (d/p "Für jede Implementierung existiert ein Namespace, der dafür verantwortlich ist, "
                  "zu verwalten, wo Übersetzungen gespeichert werden und wie ein gegebener Typ übersetzt wird.")
         :fr (d/p "Pour chaque implémentation, un espace de noms existe qui est responsable de "
                  "gérer où stocker les traductions et comment traduire un type donné.")
         :es (d/p "Para cada implementación, existe un espacio de nombres responsable de "
                  "gestionar dónde almacenar las traducciones y cómo traducir un tipo dado.")
         :hr (d/p "Za svaku implementaciju postoji imenik koji je odgovoran za "
                  "upravljanje gdje se spremaju prijevodi i kako prevesti zadani tip.")
         (d/p "For each impementation namespace exists that is responsible for "
              "managing where to store translations and how to translate given type."))
       (d/div
        {:className (css :pl-4 :pt-4 :font-bold)} (_translate :examples))
       (d/div
        {:className "table-container"}
        (d/table
         (d/thead
          (d/tr
           (map
            (fn [locale]
              (d/th {:key locale} locale))
            ["en_US" "es" "de" "ja" "zh_CN"])))
         (d/tbody
          (let [number 100000.001]
            (d/tr
             (d/td (translate number :en_US))
             (d/td (translate number :es))
             (d/td (translate number :de))
             (d/td (translate number :ja))
             (d/td (translate number :zh_CN))))
          (let [number 100000.001]
            (d/tr
             (d/td (translate number "GBP"))
             (d/td (translate number "MXN"))
             (d/td (translate number "EUR"))
             (d/td (translate number "JPY"))
             (d/td (translate number "CNY"))))
          (let [date (js/Date.)]
            (<>
             (d/tr
              (d/td (translate date :en_US))
              (d/td (translate date :es))
              (d/td (translate date :de))
              (d/td (translate date :ja))
              (d/td (translate date :zh_CN)))
             (d/tr
              (d/td (translate date :en_US :full-date))
              (d/td (translate date :es :full-date))
              (d/td (translate date :de :full-date))
              (d/td (translate date :ja :full-date))
              (d/td (translate date :zh_CN :full-date)))
             (d/tr
              (d/td (translate date :en_US :long-date))
              (d/td (translate date :es :long-date))
              (d/td (translate date :de :long-date))
              (d/td (translate date :ja :long-date))
              (d/td (translate date :zh_CN :long-date))))))))
       (case locale
         :de (d/p
              "Das Formatieren von Zahlen funktioniert auf zwei Arten. Erstens, wenn eine Schlüsselwortoption übergeben wird, "
              "die die Locale angibt, wird die Zahl in diesem Locale formatiert. "
              "Wenn eine Zeichenkette als zweite Option übergeben wird, versucht 'translate', die Zahl als Währung zu formatieren, "
              "indem nach dem angegebenen Währungscode (USD, EUR) gesucht wird.")
         :fr (d/p
              "La mise en forme des nombres fonctionne de deux manières. Tout d'abord, si une option mot-clé est passée "
              "qui spécifie la locale, alors le nombre est formaté pour cette locale. "
              "Lorsque une chaîne est passée comme deuxième option, 'translate' essaiera de formater "
              "le nombre comme une devise en recherchant le code de devise spécifié (USD, EUR).")
         :es (d/p
              "El formato de números funciona de dos maneras. Primero, si se pasa una opción con palabra clave "
              "que especifica la configuración regional, entonces el número se formatea para esa configuración. "
              "Cuando se pasa una cadena como segunda opción, 'translate' intentará formatear el número como moneda "
              "buscando el código de moneda especificado (USD, EUR).")
         :hr (d/p
              "Formatiranje brojeva radi na dva načina. Prvo, ako se proslijedi opcija ključne riječi "
              "koja specificira lokalne postavke, broj se formatira za te postavke. "
              "Kada se kao druga opcija proslijedi niz, 'translate' će pokušati formatirati broj kao valutu "
              "pretražujući navedeni kod valute (USD, EUR).")
         (d/p
          "Formatting numbers works in two ways. First if keyword option is passed that
            specifies locale, than number is formated to that locale
            When string is passed as second option, translate will try to format
            number as currency by looking for specified currency code (USD, EUR)"))
         ;;
       (translate-code-example)
         ;;
       (case locale
         :de (d/p "Folgende Formatierungsoptionen stehen für den Datentyp zur Verfügung:")
         :fr (d/p "Les options de formatage suivantes sont disponibles pour le type de date :")
         :es (d/p "Las siguientes opciones de formato están disponibles para el tipo de fecha:")
         :hr (d/p "Sljedeće opcije formatiranja dostupne su za tip datuma:")
         (d/p "Following formatting options are available for date type:"))
       (d/ul
        (d/li (d/b ":full-date"))
        (d/li (d/b ":long-date"))
        (d/li (d/b ":medium-date"))
        (d/li (d/b ":date"))
        (d/li (d/b ":full-time"))
        (d/li (d/b ":long-time"))
        (d/li (d/b ":medium-time"))
        (d/li (d/b ":time"))
        (d/li (d/b ":full-datetime"))
        (d/li (d/b ":long-datetime"))
        (d/li (d/b ":medium-datetime"))
        (d/li (d/b ":datetime"))
        (d/li (d/b ":calendar")))
       ;;
       (d/br)
       (case locale
         :de
         (<>
          (d/h4 "Übersetzungen hinzufügen")
          (d/p
           "In den vorherigen Abschnitten haben wir Zahlen und Daten übersetzt. In "
           "den meisten Fällen ist dies ausreichend, um Werte anzuzeigen, aber was ist mit "
           "Teilen der Anwendung, die statisch sind und den Kontext des Anwendungszustands bereitstellen?")
          (d/p
           "Für diese Situationen wäre es vorzuziehen, Schlüsselwörter oder "
           "UUIDs zu registrieren und Übersetzungen hinzuzufügen, damit " (d/em "toddler.i18n/translate")
           " wie erwartet funktioniert.")
          (d/p
           "So fügen Sie Übersetzungen hinzu. Im Folgenden sehen Sie den tatsächlichen Namespace der "
           "Toddler-Bibliothek, der Übersetzungen hinzufügt, die allgemein genug sind."))
         :fr
         (<>
          (d/h4 "Ajouter des traductions")
          (d/p
           "Dans les sections précédentes, nous avons traduit des nombres et des dates. Dans "
           "la plupart des cas, cela convient pour afficher des valeurs, mais qu'en est-il des "
           "parties de l'application qui sont statiques et fournissent le contexte de l'état de l'application ?")
          (d/p
           "Pour ces situations, il serait préférable d'enregistrer des mots-clés ou des "
           "UUID et d'ajouter des traductions afin que " (d/em "toddler.i18n/translate")
           " fonctionne comme prévu.")
          (d/p
           "Voici comment ajouter des traductions. Ce qui suit est un namespace réel de la "
           "bibliothèque Toddler qui ajoute des traductions suffisamment courantes."))
         :es
         (<>
          (d/h4 "Añadiendo traducciones")
          (d/p
           "En las secciones anteriores, estábamos traduciendo números y fechas. En "
           "la mayoría de los casos, esto es suficiente para mostrar valores, pero ¿qué hay de las "
           "partes de la aplicación que son estáticas y proporcionan el contexto del estado de la aplicación?")
          (d/p
           "Para estas situaciones, sería preferible registrar palabras clave o "
           "UUIDs y agregar traducciones para que " (d/em "toddler.i18n/translate")
           " funcione como se espera.")
          (d/p
           "Así es como se añaden traducciones. Lo siguiente es un espacio de nombres real de la "
           "biblioteca Toddler que añade traducciones lo suficientemente comunes."))
         :hr
         (<>
          (d/h4 "Dodavanje prijevoda")
          (d/p
           "U prethodnim odjeljcima smo prevodili brojeve i datume. U "
           "većini slučajeva to je dovoljno za prikazivanje vrijednosti, ali što je s "
           "dijelovima aplikacije koji su statični i pružaju kontekst stanja aplikacije?")
          (d/p
           "Za ove situacije bilo bi poželjno registrirati ključne riječi ili "
           "UUID-ove i dodati prijevode kako bi " (d/em "toddler.i18n/translate")
           " radio kako se očekuje.")
          (d/p
           "Evo kako dodati prijevode. Slijedi stvarni imenik iz "
           "Toddler biblioteke koji dodaje prijevode koji su dovoljno uobičajeni."))
         ;;
         (<>
          (d/h4 "Adding translations")
          (d/p
           "In previous sections we were translating numbers and dates. In "
           "most cases this is fine for displaying values, but what about "
           "part of application that is static and provides context of application "
           "state.")
          (d/p
           "For this situations it would be preferable to register keywords or "
           " UUIDs and add translations so that " (d/em "toddler.i18n/translate")
           " can work as expected.")
          (d/p
           "This is how you add translations. Following is actual namespace from "
           "toddler library that adds-translations that are common enough.")))
       (add-translations-code)
       (d/br)
       (case locale
         :de
         (<>
          (d/h4 "Locale hinzufügen")
          (d/p "Manchmal ist es unpraktisch, Übersetzungen auf diese Weise hinzuzufügen. Was ist, wenn "
               "Sie Übersetzungen basierend auf der Locale trennen und in verschiedenen "
               "Dateien speichern möchten, sodass sie bei Bedarf geladen werden können?")
          (d/p "Natürlich ist das möglich. Sehen Sie sich das an:"))
         :fr
         (<>
          (d/h4 "Ajouter une locale")
          (d/p "Parfois, il est peu pratique d'ajouter des traductions de cette manière. Et si "
               "vous vouliez séparer les traductions en fonction de la locale et les conserver dans des "
               "fichiers différents pour pouvoir les charger à la demande ?")
          (d/p "Bien sûr, c'est possible. Regardez ça :"))
         :es
         (<>
          (d/h4 "Añadir configuración regional")
          (d/p "A veces es incómodo añadir traducciones de esta manera. ¿Qué pasa si "
               "quieres separar las traducciones según la configuración regional y mantenerlas en "
               "archivos diferentes para cargarlas bajo demanda?")
          (d/p "Claro que es posible. Échale un vistazo:"))
         :hr
         (<>
          (d/h4 "Dodavanje lokalnih postavki")
          (d/p "Ponekad je nezgodno dodavati prijevode na ovaj način. Što ako "
               "želite odvojiti prijevode na temelju lokalnih postavki i držati ih u različitim "
               "datotekama kako bi se mogli učitati na zahtjev?")
          (d/p "Naravno da je to moguće. Pogledajte ovo:"))
         (<>
          (d/h4 "adding locale")
          (d/p "Sometimes it is awkward to add translations in this fashion. What if "
               "you wan't to separate translations based on locale and keep in different "
               "files. So that it can be loaded on demand.")
          (d/p "Sure that is possible. Check it out:")))
       (d/pre
        {:className "code"}
        (d/code "(add-locale
 #:default {:light \"Light\"
            :fatal.error \"Shit hit the fan!\"
            :some.specific.thing \"Gold\"})

      
 (translate :some.specific.thing :en) ;; \"Gold\"
 (translate :fatal.error :hr) ;; \"Shit hit the fan!\""))

       ;;
       (d/br)
       (case locale
         (<>
          (d/h4 "Translating components")
          (d/p
           "Sooner or later even this won't be enough to use-translate. Sometimes "
           "situation will get complex, using many keywords to translate parts "
           "sentence or text will be hard.")
          (d/p
           "In cases like this it is possible to create " (d/b "Helix") " component and "
           "add that component to translations. Then render that component in respect to "
           "selected locale.")))
       (add-component-code)
       (d/br)
       (d/h4 (_translate :result))
       ($ show-i18n-example)))))

(comment
  (translate 100000.001 :en_US) ;; "100,000.001"
  (translate 100000.001 :fr)    ;; "100 000,001"
  (translate 100000.001 "JPY")  ;; "¥100,000.00"
  (translate 100000.001 "INR")  ;; "₹100,000.00"
  (translate 100000.001 "BRL")  ;; "R$ 100,000.00"

;; Default format is short-datetime
  (translate (js/Date.) :en_US)                   ;; "1/10/25, 1:43 PM"
  (translate (js/Date.) :en_US :date)             ;; "1/10/25"
  (translate (js/Date.) :en_US :full-datetime)    ;; "Friday, January 10, 2025 at 1:44:27 PM UTC+1"
  (translate (js/Date.) :en_US :medium-datetime)  ;; "Jan 10, 2025, 1:45:06 PM"

  ;;
  (translate :some.specific.thing :en) ;; "Gold"
  (translate :fatal.error :hr) ;; "Shit hit the fan!"
  )

(add-locale
 #:default {:light "Light"
            :fatal.error "Shit hit the fan!"
            :some.specific.thing "Gold"})

(add-translations
 (merge
  #:showcase.i18n {:default "i18n"}
  #:showcase.i18n.info {:default i18nStory}))

(defnc i18n []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "40rem"}}
             ($ (translate :showcase.i18n.info)))))))
