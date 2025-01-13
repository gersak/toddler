(ns toddler.showcase.i18n
  (:require
   ["react-dom" :as rdom]
   [clojure.core.async :as async]
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
   [toddler.md :as md]
   [toddler.core :as toddler]
   [toddler.showcase.common :refer [$info use-code-refresh]]))

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
        :hr
        (d/p "Pozdrav, ovo je primjer komponente. "
             "Pokušajte promijeniti lokalne postavke da vidite kako će se ova rečenica promijeniti.")
        (d/p "hello this is example component. "
             "Try and change locale to see how this sentence will change"))))))

(defnc hooks-example []
  (let [locale (toddler/use-current-locale)
        translate (toddler/use-translate)
        translatef (toddler/use-translatef)]
    (<>
     (d/br)
     (d/h4 "React Hooks (Helix)"))))

(add-locale
 #:default {:light "Light"
            :fatal.error "Shit hit the fan!"
            :some.specific.thing "Gold"})

(add-translations
 (merge
  #:showcase.i18n {:default "i18n"}))

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
             ($ md/watch-url {:url "/doc/en/i18n.md"})
             (when-some [el (.getElementById js/document "component-translation-example")]
               (rdom/createPortal
                ($ i18n-example)
                el)))))))
