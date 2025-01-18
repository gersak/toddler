(ns toddler.showcase.layout
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.layout :as layout]
   [toddler.i18n.keyword :refer [add-translations]]
   [toddler.router :as router]))

(add-translations
 (merge
  #:showcase.layout {:default "Layout"
                     :hr "Raspored"}
      ;;
  #:showcase.tables {:default "Tables"
                     :hr "Tablice"}
      ;;
  #:button.default {:default "Default"
                    :hr "Normalan"}
      ;;
  #:button.positive {:hr "Pozitivan"
                     :default "Positive"}
      ;;
  #:button.negative {:hr "Negativan"
                     :default "Negative"}
      ;;
  #:button.fun {:hr "Fora"
                :default "Fun"}
      ;;
  #:button.fresh {:hr "Svježe"
                  :default "Fresh"}
      ;;
  #:button.stale {:hr "Ustajalo"
                  :default "Stale"}
      ;;
  #:button.disabled {:hr "Onemogućeno"
                     :default "Disabled"}

  #:checklist.horse {:default "Horse"
                     :hr "Konj"}
  #:checklist.sheep {:default "Sheep"
                     :hr "Ovca"}
  #:checklist.cow {:default "Cow"
                   :hr "Krava"}
  #:checklist.boar {:default "Boar"
                    :hr "Vepar"}))

(defnc Layout
  {:wrap [(router/wrap-rendered :toddler.layout)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/simplebar
       {:className "fields"
        :style {:height height
                :width width
                :boxSizing "border-box"}}
       (d/div
        {:className (css :flex)}
        ($ ui/row
           {:position :center
            :className (css :bg-red-100)}
           ($ ui/column
              {:className (css :bg-blue-500)
               :style {:max-width 500}}
              (map
               (fn [idx]
                 ($ ui/row
                    {:key idx
                     :style {:height 100}
                     :position :explode
                     :className (css :bg-green-400 :items-center)}
                    (d/div "green")
                    ($ ui/column
                       {:position :end
                        :style {:width 50
                                :max-width 50}}
                       (map
                        (fn [idx]
                          ($ ui/row
                             {:key idx
                              :style {:height 20
                                      :width 40}
                              :className (css :bg-green-100)}
                             #_($ ui/input-field)))
                        (range 3)))))
               (range 10))))))))
