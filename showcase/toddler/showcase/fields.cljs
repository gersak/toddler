(ns toddler.showcase.fields
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.layout :as layout]
   [toddler.ui.components :as components]
   [toddler.core :refer [use-translate]]
   [toddler.i18n.keyword :refer [add-translations]]
   [toddler.dev :as dev]))

(add-translations
 (merge
  #:chose {:default "Choose"
           :hr "Odaberi"}
  #:not-available {:default "N/A"
                   :hr "-"}
  #:showcase.fields {:default "Fields"
                     :hr "Polja"}
      ;;
  #:showcase.tables {:default "Tables"
                     :hr "Tablice"}
      ;;
  #:button.default {:default "Normal"
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
                    :hr "Vepar"}
  #:showcase.fields.input
   {:default "Input Field"
    :hr "Unos Polje"}
  #:showcase.fields.password
   {:default "Password Field"
    :hr "Lozinka Polje"}
  #:showcase.fields.boolean
   {:default "Checkbox Field"
    :hr "Označeno polje"}
  #:showcase.fields.float
   {:default "Float Field"
    :hr "Decimalno polje"}
  #:showcase.fields.int
   {:default "Integer Field"
    :hr "Prirodni broj polje"}
  #:showcase.fields.multiselect
   {:default "Multiselect Field"
    :hr "Višeznačno polje"}
  #:showcase.fields.dropdown
   {:default "Dropdown Field"
    :hr "Polje sa padajučim izbornikom"}
  #:showcase.fields.text
   {:default "Text Field"
    :hr "Tekst polje"}
  #:showcase.fields.date
   {:default "Date field"
    :hr "Datum Polje"}
  #:showcase.fields.datetime
   {:default "Timestamp field"
    :hr "Vremensko polje"}
  #:showcase.fields.date-period
   {:default "Date Period Field"
    :hr "Period polje"}
  #:showcase.fields.datetime-period
   {:default "Date Time Period Field"
    :hr "Polje Vremenskog Perioda"}
  #:showcase.fields.identity
   {:default "Identity Field"
    :hr "Identitet Polje"}
  #:showcase.fields.identity-multiselect
   {:default "Identity Multiselect Field"
    :hr "Višeznačno Identitet Polje"}
  #:showcase.fields.in-row
   {:default "In Row "
    :hr "Isti Redak "}
  #:option-0 {:default "zero" :hr "nula"}
  #:option-1 {:default "one" :hr "jedan"}
  #:option-2 {:default "two" :hr "dva"}
  #:option-3 {:default "three" :hr "tri"}
  #:option-4 {:default "four" :hr "četiri"}
  #:option-5 {:default "five" :hr "pet"}
  #:option-6 {:default "six" :hr "šest"}
  #:option-7 {:default "seven" :hr "sedam"}
  #:option-8 {:default "eight" :hr "osam"}
  #:option-9 {:default "nine" :hr "devet"}
  #:option-10 {:default "ten" :hr "deset"}
  #:option-11 {:default "eleven" :hr "jedanaest"}
  #:option-12 {:default "twelve" :hr "dvanaest"}
  #:option-13 {:default "thirteen" :hr "trinaest"}
  #:option-14 {:default "fourteen" :hr "četrnaest"}
  #:option-15 {:default "fifteen" :hr "petnaest"}
  #:option-16 {:default "sixteen" :hr "šesnaest"}
  #:option-17 {:default "seventeen" :hr "sedamnaest"}
  #:option-18 {:default "eighteen" :hr "osamnaest"}
  #:option-19 {:default "nineteen" :hr "devetnaest"}
  #:option-20 {:default "twenty" :hr "dvadeset"}))

(def test-options
  (map
   (fn [v]
     {:value v
      :context (cond
                 (zero? (mod v 5)) :positive
                 (zero? (mod v 8)) :negative
                 :else :normal)})
   (range 20)))

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
                                             :multiselect-field []
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
          ($ ui/row
             {:position :center}
             ($ ui/row
                {:style {:max-width 500}}
                ($ ui/column
                   {:label "Fields showcase"
                    :className (css
                                ["& > .toddler-row" :my-2])}
                   ($ ui/row
                      {:label "Buttons"
                       :className (css :flex-wrap)}
                      ($ ui/button (translate :button.default))
                      ($ ui/button {:class "positive"} (translate :button.positive))
                      ($ ui/button {:class "negative"} (translate :button.negative))
                      ; ($ ui/button {:class "fun"} (translate :button.fun))
                      ; ($ ui/button {:class "fresh"} (translate :button.fresh))
                      ; ($ ui/button {:class "stale"} (translate :button.stale))
                      ($ ui/button {:disabled true} (translate :button.disabled)))
                   ($ ui/row
                      ($ ui/input-field
                         {:name (translate :showcase.fields.input)
                          :value (:free-input state)
                          :onChange (fn [v] (set-state! assoc :free-input v))}))
                   ($ ui/row
                      ($ ui/password-field
                         {:name (translate :showcase.fields.password)
                          :value (:password state)
                          :onChange (fn [v] (set-state! assoc :password v))}))
                   ($ ui/row
                      ($ ui/boolean-field
                         {:name (translate :showcase.fields.boolean)
                          :value (:boolean-field state)
                          :onChange (fn [] (set-state! update :boolean-field not))}))
                   ($ ui/row
                      ($ ui/integer-field
                         {:name (translate :showcase.fields.int)
                          :value (:integer-field state)
                          :onChange (fn [v] (set-state! assoc :integer-field v))}))
                   ($ ui/row
                      ($ ui/float-field
                         {:name (translate :showcase.fields.float)
                          :value (:float-field state)
                          :onChange (fn [v] (set-state! assoc :float-field v))}))
                   #_($ ui/row
                        ($ ui/currency-field
                           {:name "Currency field"
                            :value (:currency-field state)
                            :onChange (fn [v] (set-state! assoc :currency-field v))}))
                   ($ ui/row
                      ($ ui/multiselect-field
                         {:name (translate :showcase.fields.multiselect)
                          :value (:multiselect-field state)
                          :placeholder (translate :chose)
                          :search-fn #(when-let [value (:value %)]
                                        (let [option-key (keyword (str "option-" value))]
                                          (translate option-key)))
                          :context-fn :context
                          :options test-options
                          :onRemove (fn [v] (set-state! assoc :multiselect-field v))
                          :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
                   ($ ui/row
                      ($ ui/dropdown-field
                         {:name (translate :showcase.fields.dropdown)
                          :value (:dropdown-field state)
                          :search-fn #(when-let [value (:value %)]
                                        (let [option-key (keyword (str "option-" value))]
                                          (translate option-key)))
                          :context-fn :context
                          :options test-options
                          :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
                   ($ ui/row
                      ($ ui/text-field
                         {:name (translate :showcase.fields.text)
                          :value (:textarea-field state)
                          :onChange (fn [v] (set-state! assoc :textarea-field v))}))
                   ($ ui/row
                      ($ ui/date-field
                         {:name (translate :showcase.fields.date)
                          :value (:date-field state)
                          :onChange #(set-state! assoc :date-field %)}))
                   ($ ui/row
                      ($ ui/timestamp-field
                         {:name (translate :showcase.fields.datetime)
                          :value (:timestamp-field state)
                          :onChange #(set-state! assoc :timestamp-field %)}))
                   ($ ui/row
                      ($ ui/date-period-field
                         {:name (translate :showcase.fields.date-period)
                          :value (:date-period-field state)
                          :onChange (fn [v] (set-state! assoc :date-period-field v))}))
                   ($ ui/row
                      ($ ui/timestamp-period-field
                         {:name (translate :showcase.fields.datetime-period)
                          :value (:timestamp-period-field state)
                          :onChange (fn [v] (set-state! assoc :timestamp-period-field v))}))
                   ($ ui/row
                      ($ ui/identity-field
                         {:name (translate :showcase.fields.identity)
                          :value (:identity-field state)
                          :placeholder (translate :chose)
                          :options [{:name "John"}
                                    {:name "Harry"}
                                    {:name "Ivan"}]
                          :onChange (fn [v] (set-state! assoc :identity-field v))}))
                   ($ ui/row
                      ($ ui/identity-multiselect-field
                         {:name (translate :showcase.fields.identity-multiselect)
                          :value (:identity-multiselect-field state)
                          :placeholder (translate :chose)
                          :options [{:name "John"}
                                    {:name "Harry"}
                                    {:name "Ivan"}
                                    {:name "Kiki"}
                                    {:name "Rita"}
                                    {:name "Tia"}]
                          :onRemove (fn [v] (set-state! assoc :identity-multiselect-field v))
                          :onChange (fn [v] (set-state! assoc :identity-multiselect-field v))}))
                   ($ ui/row
                      {:className (css
                                   ["& .toddler-field:not(:first-child)" :ml-2])}
                      ($ ui/input-field
                         {:name (str (translate :showcase.fields.in-row) 1)})
                      ($ ui/input-field
                         {:name (str (translate :showcase.fields.in-row) 2)})
                      ($ ui/input-field
                         {:name (str (translate :showcase.fields.in-row) 3)})))))))))
