(ns toddler.showcase.inputs
  (:require
   [helix.core :refer [$ defnc <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.dev :as dev]
   [toddler.elements :as toddler]))


; (defnc AvatarImage
;   []
;   (let [[state set-state!] (hooks/use-state 100)]
;     (<>
;      ($ toddler/slider
;         {:width "300px"
;          :min "10"
;          :max "500"
;          :value (str state)
;          :onChange (fn [e] (set-state! (.-value (.-target e))))})
;      (d/br)
;      ($ toddler/avatar
;         {:size (int state)
;          :avatar "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"}))))


; (dev/add-component
;  {:key ::avatar-image
;   :name "Avatar image"
;   :render AvatarImage})



(defnc InputTypes
  []
  #_(let [[state set-state!] (hooks/use-state {:number-input 0
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
     ($ toddler/row "Number input"
        ($ toddler/number-input
           {:value (:number-input state)
            :onChange (fn [e] (set-state! (assoc state :number-input (int (.. e -target -value)))))}))
     ($ toddler/row
        ($ toddler/input-field
           {:name "Auto-size free input"
            :value (:free-input state)
            :onChange (fn [e] (set-state! (assoc state :free-input (.. e -target -value))))}))
     ($ toddler/row
        ($ toddler/autosize-text
           {:name "Auto-size text"
            :value (:auto-size-input state)
            :onChange (fn [e] (set-state! (assoc state :auto-size-input (.. e -target -value))))}))
     ($ toddler/row
        ($ toddler/idle-input
           {:name "Idle input"
            :value (:idle-input state)
            :onChange (fn [e] (set-state! (assoc state :idle-input (.. e -target -value))))}))
     #_($ toddler/row
          ($ toddler/mask-input
             {:name "Mask input"
              :value (:mask-input state)
              :onChange (fn [e] (set-state! (assoc state :mask-input (.. e -target -value))))}))
     #_($ toddler/row "Default field"
          ($ toddler/field-row "hey")
          ($ toddler/field-row "hey"))
     ($ toddler/row
        ($ toddler/checkbox-field
           {:name "Checkbox field"
            :active (:check-box state)
            :onClick (fn [] (set-state! update :check-box not))}))
     ($ toddler/row
        ($ toddler/integer-field
           {:name "Integer field"
            :value (:integer-field state)
            :onChange (fn [v] (set-state! assoc :integer-field v))}))
     ($ toddler/row
        ($ toddler/float-field
           {:name "Float field"
            :value (:float-field state)
            :onChange (fn [v] (set-state! assoc :float-field v))}))
     ($ toddler/row
        ($ toddler/multiselect-field
           {:name "Multi-select field"
            :value (:multiselect-field state)
            :new-fn identity
            :placeholder "Add item"
            :options ["sto" "dvijesto" "tristo"]
            :onRemove (fn [v] (set-state! assoc :multiselect-field v))
            :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
     ($ toddler/row
        ($ toddler/dropdown-field
           {:name "Dropdown field"
            :value (:dropdown-field state)
            :new-fn identity
            :placeholder "Maybe pick item"
            :options ["sto" "dvijesto" "tristo"]
            :onRemove (fn [v] (set-state! assoc :dropdown-field v))
            :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
     ($ toddler/row
        ($ toddler/textarea-field
           {:name "Text area field"
            :value (:textarea-field state)
            :onChange (fn [e] (set-state! assoc :textarea-field (.. e -target -value)))}))

     ($ toddler/row
        ($ toddler/TimestampDropdownElement
           {:name "Popup calendar"
            :placeholder "Click to open calendar"}))
     #_($ toddler/row "Period input"
          ($ toddler/PeriodInput
             {:value "12.3.2022."
              :onChange (fn [e] (set-state! assoc :textarea-field (.. e -target -value)))})))))


(dev/add-component
 {:key ::input-types
  :name "Input types"
  :render InputTypes})


; (defnc PeriodInput
;   []
;   (let [[state set-state!] (hooks/use-state [(js/Date.) (js/Date. "2022-10-01T12:00:00")])]
;     ($ toddler/PeriodDropdownElement
;        {:value state
;         :onChange (fn [v] (set-state! v))})))


; (dev/add-component
;  {:key ::period-input
;   :name "Period input"
;   :render PeriodInput})
