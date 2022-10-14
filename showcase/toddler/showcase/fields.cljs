(ns toddler.showcase.fields
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   ; [helix.dom :as d]
   [toddler.ui :as ui]
   [toddler.avatar :as a]
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
                                             :period-input 123213213})]
     ($ a/Generator
        ($ default/Provider
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
              ($ ui/timestamp-field
                 {:name "Timestamp field"
                  :value (:timestamp-field state)
                  :placeholder "Click to open calendar"
                  :onChange #(set-state! assoc :timestamp-field %)}))
           ($ ui/row
              ($ ui/period-field
                 {:name "Period Field"
                  :placeholder "Click to open period dropdown"
                  :value (:period-field state)
                  :onChange (fn [v] (set-state! assoc :period-field v))}))
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
                  :onChange (fn [v] (set-state! assoc :identity-multiselect-field v))}))))))



(dev/add-component
   {:key ::fields
    :name "Fields"
    :render Fields})


(defnc Table
   []
   "hi")


(dev/add-component
   {:key ::table
    :name "Table"
    :render Table})
