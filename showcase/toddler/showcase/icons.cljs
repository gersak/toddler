(ns toddler.showcase.icons
  (:require
   [helix.core :refer [defnc $ <>]]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.ui :as ui]
   [toddler.md.lazy :as md]
   [toddler.showcase.icons.material :as material]
   [toddler.showcase.icons.fav6 :as fav6]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 #:showcase.icons {:default "Icons"
                   :hr "Ikone"})

(declare outlined)

(defnc display-icons
  [{:keys [height icons]}]
  ($ ui/simplebar
     {:style {:height (- height 200)}
      :className (css :pt-4)}
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
       icons))))

(defnc Icons
  []
  (let [{:keys [height]} (layout/use-container-dimensions)]
    (d/div
     {:className (css :flex :flex-col)}
     ($ md/watch-url {:url "/doc/en/icons.md"})
     ($ ui/tabs
        ($ ui/tab
           {:id ::material-outlined
            :name "Material Outlined"}
           ($ display-icons {:height height :icons material/outlined}))
        ($ ui/tab
           {:id ::material-round
            :name "Material Round"}
           ($ display-icons {:height height :icons material/round}))
        ($ ui/tab
           {:id ::material-sharp
            :name "Material Sharp"}
           ($ display-icons {:height height :icons material/sharp}))
        ($ ui/tab
           {:id ::fav6-regular
            :name "FA Regular"}
           ($ display-icons {:height height :icons fav6/regular}))
        ($ ui/tab
           {:id ::fav6-solid
            :name "FA Solid"}
           ($ display-icons {:height height :icons fav6/solid}))
        ($ ui/tab
           {:id ::fav6-brands
            :name "FA Brands"}
           ($ display-icons {:height height :icons fav6/brands}))))))
