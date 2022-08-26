(ns toddler.showcase
  (:require
   ["react-dom/client" :refer [createRoot]]
   [helix.core :refer [$ defnc <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.dev :as dev]
   [toddler.interactions :as interactions]))


(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render @root ($ dev/playground))))


(defnc AutosizeInput
  []
  (let [[state set-state!] (hooks/use-state "")]
    (letfn [(on-change [e]
              (set-state! (.. e -target -value)))]
      ($ dev/centered-component
         ($ interactions/input-field
            {:name "user name"
             :value state
             :onChange on-change})))))


(dev/add-component
 {:key ::autosize-input
  :name "AutosizeInput"
  :render AutosizeInput})


(defnc AvatarImage
  []
  (let [[state set-state!] (hooks/use-state 100)]
    ($ dev/centered-component
       (<>
        ($ interactions/slider
           {:width "300px"
            :min "10"
            :max "500"
            :value (str state)
            :onChange (fn [e] (set-state! (.-value (.-target e))))})
        (d/br)
        ($ interactions/avatar
           {:size (int state)
            :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/1920px-Image_created_with_a_mobile_phone.png"})))))


(dev/add-component
 {:key ::avatar-image
  :name "Avatar image"
  :render AvatarImage})


(defn TimestampCalendar
  []
  (let [[state set-state!] (hooks/use-state nil)]
    ($ interactions/TimestampCalendarElement
       {:value state
        :onChange (fn [v] (set-state! v))})))


(dev/add-component
 {:key ::timestamp-calendar
  :name "Timestamp Calendar"
  :render TimestampCalendar})

(defn ^:export InputTypes
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
     ($ interactions/row "Number input"
        ($ interactions/number-input
           {:value (:number-input state)
            :onChange (fn [e] (set-state! (assoc state :number-input (.. e -target -value))))}))
     ($ interactions/row
        ($ interactions/input-field
           {:name "Auto-size free input"
            :value (:free-input state)
            :onChange (fn [e] (set-state! (assoc state :free-input (.. e -target -value))))}))
     ($ interactions/row
        ($ interactions/autosize-text
           {:name "Auto-size text"
            :value (:auto-size-input state)
            :onChange (fn [e] (set-state! (assoc state :auto-size-input (.. e -target -value))))}))
     ($ interactions/row
        ($ interactions/idle-input
           {:name "Idle input"
            :value (:idle-input state)
            :onChange (fn [e] (set-state! (assoc state :idle-input (.. e -target -value))))}))
     #_($ interactions/row
          ($ interactions/mask-input
             {:name "Mask input"
              :value (:mask-input state)
              :onChange (fn [e] (set-state! (assoc state :mask-input (.. e -target -value))))}))
     #_($ interactions/row "Default field"
          ($ interactions/field-row "hey")
          ($ interactions/field-row "hey"))
     ($ interactions/row
        ($ interactions/checkbox-field
           {:name "Checkbox field"
            :active (:check-box state)
            :onClick (fn [] (set-state! update :check-box not))}))
     ($ interactions/row
        ($ interactions/integer-field
           {:name "Integer field"
            :value (:integer-field state)
            :onChange (fn [v] (set-state! assoc :integer-field v))}))
     ($ interactions/row
        ($ interactions/float-field
           {:name "Float field"
            :value (:float-field state)
            :onChange (fn [v] (set-state! assoc :float-field v))}))
     ($ interactions/row
        ($ interactions/multiselect-field
           {:name "Multi-select field"
            :value (:multiselect-field state)
            :new-fn identity
            :placeholder "Add item"
            :options ["sto" "dvijesto" "tristo"]
            :onRemove (fn [v] (set-state! assoc :multiselect-field v))
            :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
     ($ interactions/row
        ($ interactions/dropdown-field
           {:name "Dropdown field"
            :value (:dropdown-field state)
            :new-fn identity
            :placeholder "Maybe pick item"
            :options ["sto" "dvijesto" "tristo"]
            :onRemove (fn [v] (set-state! assoc :dropdown-field v))
            :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
     ($ interactions/row
        ($ interactions/textarea-field
           {:name "Text area field"
            :value (:textarea-field state)
            :onChange (fn [e] (set-state! assoc :textarea-field (.. e -target -value)))}))
     #_($ interactions/row "Period input"
          ($ interactions/PeriodInput
             {:value "12.3.2022."
              :onChange (fn [e] (set-state! assoc :textarea-field (.. e -target -value)))})))))

(dev/add-component
 {:key ::input-types
  :name "Input types"
  :render InputTypes})
