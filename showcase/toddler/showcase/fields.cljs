(ns toddler.showcase.fields
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.avatar :as a]
   [toddler.layout :as layout]
   [toddler.ui.components :as components]
   [toddler.hooks :refer [use-translate]]
   [toddler.i18n.keywords :refer [add-translations]]
   [toddler.dev :as dev]))


(add-translations
   (merge
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


(defnc Fields
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
     ($ a/Generator
        {:className (css {:visibility "hidden" :position "fixed" :top "0px" :left "0px"})}
        ($ components/Provider
           ($ ui/simplebar
              {:className "fields"
               :style {:height height
                       :width width
                       :boxSizing "border-box"}}
              ($ ui/row
                 {:label "Buttons"}
                 ($ ui/button (translate :button.default))
                 ($ ui/button {:context :positive} (translate :button.positive))
                 ($ ui/button {:context :negative} (translate :button.negative))
                 ($ ui/button {:context :fun} (translate :button.fun))
                 ($ ui/button {:context :fresh} (translate :button.fresh))
                 ($ ui/button {:context :stale} (translate :button.stale))
                 ($ ui/button {:disabled true} (translate :button.disabled)))
              ($ ui/row
                 ($ ui/input-field
                    {:name "Auto-size free input"
                     :value (:free-input state)
                     :onChange (fn [v] (set-state! assoc :free-input v))}))
              ($ ui/row
                 ($ ui/boolean-field
                    {:name "Checkbox field"
                     :value (:boolean-field state)
                     :onClick (fn [] (set-state! update :boolean-field not))}))
              ($ ui/row
                 ($ ui/checklist-field
                    {:name "Checklist field"
                     :value (:checklist-field state)
                     :multiselect? true
                     :options [{:name (translate :checklist.horse)
                                :value :konj}
                               {:name (translate :checklist.sheep)
                                :value :ovca}
                               {:name (translate :checklist.cow)
                                :value :krava}
                               {:name (translate :checklist.boar)
                                :value :vepar}]
                     :onChange (fn [v] (set-state! assoc :checklist-field v))}))
              ($ ui/row
                 ($ ui/integer-field
                    {:name "Integer field"
                     :value (:integer-field state)
                     :onChange (fn [v] (set-state! assoc :integer-field v))}))
              ($ ui/row
                 ($ ui/float-field
                    {:name "Float field"
                     :value (:float-field state)
                     :onChange (fn [v] (set-state! assoc :float-field v))}))
              ($ ui/row
                 ($ ui/currency-field
                    {:name "Currency field"
                     :value (:currency-field state)
                     :onChange (fn [v] (set-state! assoc :currency-field v))}))
              ($ ui/row
                 ($ ui/multiselect-field
                    {:name "Multi-select field"
                     :value (:multiselect-field state)
                     :options ["sto" "dvijesto" "tristo"]
                     :onRemove (fn [v] (set-state! assoc :multiselect-field v))
                     :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
              ($ ui/row
                 ($ ui/dropdown-field
                    {:name "Dropdown field"
                     :value (:dropdown-field state)
                     :new-fn identity
                     :options ["sto" "dvijesto" "tristo"]
                     :onRemove (fn [v] (set-state! assoc :dropdown-field v))
                     :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
              ($ ui/row
                 ($ ui/text-field
                    {:name "Text area field"
                     :value (:textarea-field state)
                     :onChange (fn [e] (set-state! assoc :textarea-field (.. e -target -value)))}))
              ($ ui/row
                 ($ ui/date-field
                    {:name "Date field"
                     :value (:date-field state)
                     :onChange #(set-state! assoc :date-field %)}))
              ($ ui/row
                 ($ ui/timestamp-field
                    {:name "Timestamp field"
                     :value (:timestamp-field state)
                     :onChange #(set-state! assoc :timestamp-field %)}))
              ($ ui/row
                 ($ ui/date-period-field
                    {:name "Period Field"
                     :value (:date-period-field state)
                     :onChange (fn [v] (set-state! assoc :date-period-field v))}))
              ($ ui/row
                 ($ ui/timestamp-period-field
                    {:name "Timestamp Period Field"
                     :value (:timestamp-period-field state)
                     :onChange (fn [v] (set-state! assoc :timestamp-period-field v))}))
              ($ ui/row
                 ($ ui/identity-field
                    {:name "Identity Field"
                     :value (:identity-field state)
                     :options [{:name "John"}
                               {:name "Harry"}
                               {:name "Ivan"}]
                     :onChange (fn [v] (set-state! assoc :identity-field v))}))
              ($ ui/row
                 ($ ui/identity-multiselect-field
                    {:name "Identity Multiselect Field"
                     :value (:identity-multiselect-field state)
                     :options [{:name "John"}
                               {:name "Harry"}
                               {:name "Ivan"}]
                     :onRemove (fn [v] (set-state! assoc :identity-multiselect-field v))
                     :onChange (fn [v] (set-state! assoc :identity-multiselect-field v))})))))))



(dev/add-component
   {:key ::fields
    :name "Fields"
    :render Fields})
