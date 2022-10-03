(ns toddler.showcase.ui
  (:require
   [helix.core :refer [$ defnc <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.ui :as ui]
   [toddler.ui.default :as default]
   [toddler.dev :as dev]
   [toddler.elements :as toddler]))


(defnc InputTypes
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
                                             :period-input 123213213})]
    (<>
     ($ toddler/row
        ($ ui/input-field
           {:name "Auto-size free input"
            :value (:free-input state)
            :onChange (fn [e] (set-state! (assoc state :free-input (.. e -target -value))))}))
     ($ toddler/row
        ($ toddler/checkbox-field
           {:name "Checkbox field"
            :active (:check-box state)
            :onClick (fn [] (set-state! update :check-box not))}))
     ($ toddler/row
        ($ ui/integer-field
           {:name "Integer field"
            :value (:integer-field state)
            :onChange (fn [v] (set-state! assoc :integer-field v))}))
     ($ toddler/row
        ($ ui/float-field
           {:name "Float field"
            :value (:float-field state)
            :onChange (fn [v] (set-state! assoc :float-field v))}))
     ($ toddler/row
        ($ ui/multiselect-field
           {:name "Multi-select field"
            :value (:multiselect-field state)
            :placeholder "Add item"
            :options ["sto" "dvijesto" "tristo"]
            :onRemove (fn [v] (set-state! assoc :multiselect-field v))
            :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
     ($ toddler/row
        ($ ui/dropdown-field
           {:name "Dropdown field"
            :value (:dropdown-field state)
            :new-fn identity
            :placeholder "Maybe pick item"
            :options ["sto" "dvijesto" "tristo"]
            :onRemove (fn [v] (set-state! assoc :dropdown-field v))
            :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
     ($ toddler/row
        ($ ui/text-field
           {:name "Text area field"
            :value (:textarea-field state)
            :onChange (fn [e] (set-state! assoc :textarea-field (.. e -target -value)))}))

     ($ toddler/row
        ($ ui/timestamp-field
           {:name "Timestamp field"
            :value (:timestamp-field state)
            :placeholder "Click to open calendar"
            :onChange #(set-state! assoc :timestamp-field %)}))
     ($ toddler/row
        ($ ui/period-field
           {:name "Period input"
            :value (:period-field state)
            :onChange (fn [v]
                         (.log js/console "Setting period: " v)
                         (set-state! assoc :period-field v))})))))


(defn Default
   []
   ($ default/Provider
      ($ InputTypes)))


(dev/add-component
 {:key ::ui-testing
  :name "UI"
  :render Default})
