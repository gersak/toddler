(ns toddler.showcase.fields
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.avatar :as a]
   [toddler.layout :as layout]
   [toddler.ui.default :as default]
   [toddler.dev :as dev]))


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
        {:keys [height width]} (layout/use-container-dimensions)]
     ($ a/Generator
        {:className (css {:visibility "hidden" :position "fixed" :top "0px" :left "0px"})}
        ($ default/Provider
           ($ ui/simplebar
              {:className "fields"
               :style {:height height
                       :width width
                       :boxSizing "border-box"}}
              ($ ui/row
                 {:label "Buttons"}
                 ($ ui/button "Default")
                 ($ ui/button {:context :positive} "Positive")
                 ($ ui/button {:context :negative} "Negative")
                 ($ ui/button {:context :fun} "Fun")
                 ($ ui/button {:context :fresh} "Fresh")
                 ($ ui/button {:context :stale} "Stale")
                 ($ ui/button {:disabled true} "Disabled"))
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
                     :options [{:name "Konj"
                                :value :konj}
                               {:name "Ovca"
                                :value :ovca}
                               {:name "Krava"
                                :value :krava}
                               {:name "Vepar"
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
                     :placeholder "Price"
                     :value (:currency-field state)
                     :onChange (fn [v] (set-state! assoc :currency-field v))}))
              ($ ui/row
                 ($ ui/multiselect-field
                    {:name "Multi-select field"
                     :value (:multiselect-field state)
                     :placeholder "Add item"
                     :options ["sto" "dvijesto" "tristo"]
                     :onRemove (fn [v] (set-state! assoc :multiselect-field v))
                     :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
              ($ ui/row
                 ($ ui/dropdown-field
                    {:name "Dropdown field"
                     :value (:dropdown-field state)
                     :new-fn identity
                     :placeholder "Maybe pick item"
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
                     :placeholder "Click to open calendar"
                     :onChange #(set-state! assoc :date-field %)}))
              ($ ui/row
                 ($ ui/timestamp-field
                    {:name "Timestamp field"
                     :value (:timestamp-field state)
                     :placeholder "Click to open timestamp calendar"
                     :onChange #(set-state! assoc :timestamp-field %)}))
              ($ ui/row
                 ($ ui/date-period-field
                    {:name "Period Field"
                     :placeholder "Click to open period dropdown"
                     :value (:date-period-field state)
                     :onChange (fn [v] (set-state! assoc :date-period-field v))}))
              ($ ui/row
                 ($ ui/timestamp-period-field
                    {:name "Timestamp Period Field"
                     :placeholder "Click to open timestamp period dropdown"
                     :value (:timestamp-period-field state)
                     :onChange (fn [v] (set-state! assoc :timestamp-period-field v))}))
              ($ ui/row
                 ($ ui/identity-field
                    {:name "Identity Field"
                     :placeholder "Click select identity"
                     :value (:identity-field state)
                     :options [{:name "John"}
                               {:name "Harry"}
                               {:name "Ivan"}]
                     :onChange (fn [v] (set-state! assoc :identity-field v))}))
              ($ ui/row
                 ($ ui/identity-multiselect-field
                    {:name "Identity Multiselect Field"
                     :placeholder "Click select identity"
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
