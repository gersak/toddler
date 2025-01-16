(ns toddler.showcase.icons
  (:require
   [helix.core :refer [defnc $ <>]]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.ui :as ui]
   [toddler.md.lazy :as md]
   [toddler.showcase.icons.material :as material]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 #:showcase.icons {:default "Icons"
                   :hr "Ikone"})

(declare outlined)

(defn show-icon
  [icon-symbol])

(defnc Icons
  []
  (let [{:keys [height]} (layout/use-container-dimensions)]
    (d/div
     {:className (css :flex :flex-col)}
     ($ md/watch-url {:url "/doc/en/icons.md"})
     ($ ui/simplebar
        {:style {:height (- height 150)}}
        (d/div
         {:className (css
                      :flex :flex-wrap
                      ["& .icon-wrapper" :m-4 :flex :flex-col :justify-center :items-center]
                      ["& .icon" {:font-size "24px"} :cursor-pointer]
                      ["& .name" :font-semibold {:font-size "10px"}])}
         (map
          (fn [[name icon]]
            ($ ui/tooltip
               {:key name
                :message (d/div {:className "name"} name)}
               (d/div
                {:className "icon-wrapper"}
                ($ icon {:className "icon"
                         :onClick (fn [] (.writeText js/navigator.clipboard name))}))))
          material/sharp))))))
