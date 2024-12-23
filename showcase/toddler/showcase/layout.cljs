(ns toddler.showcase.layout
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.layout :as layout]
   [toddler.ui.components :as components]
   [toddler.hooks :refer [use-translate]]
   [toddler.i18n.keyword :refer [add-translations]]
   [toddler.dev :as dev]))

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
  []
  (let [[state set-state!] (hooks/use-state {:number-input 0
                                             :free-input ""
                                             :check-box false
                                             :auto-size-input ""
                                             :idle-input ""
                                             :mask-input ""
                                             :integer-field 25000000
                                             :float-field 2.123543123123
                                             :multiselect-field ["jedan" "dva" "tri"]
                                             :textarea-field "I am text"
                                             :period-input 123213213})
        {:keys [height width]} (layout/use-container-dimensions)
        translate (use-translate)]
    ($ components/Provider
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
                  (range 10)))))))))

(dev/add-component
 {:id :showcase.layout
  :name "Layout"
  :segment "layout"
  :render Layout})
