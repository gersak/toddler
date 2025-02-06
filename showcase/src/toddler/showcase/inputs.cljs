(ns toddler.showcase.inputs
  (:require
   [helix.core :refer [$ defnc <>]]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.ui :as ui :refer [!]]
   [toddler.core :as toddler]
   [toddler.md.lazy :as md]
   [toddler.layout :as layout]
   [toddler.router :as router]))

(defnc buttons
  []
  (! :row
     {:align :center
      :class [(css :flex-wrap) "example-field"]}
     (! :button "Default")
     (! :button {:class "positive"} "Positive")
     (! :button {:class "negative"} "Negative")
     (! :button {:disabled true} "Disabled")))

(defnc value-fields
  []
  (let [[state set-state!]
        (hooks/use-state
         {:number-input 0
          :free-input ""
          :check-box false
          :integer-field 25000000
          :float-field 2.123543123123
          :textarea-field "I am text. Try ENTER"})]
    (<>
     (! :row
        {:className "example-field"}
        (! :field/input
           {:name "Input Field"
            :value (:free-input state)
            :onChange (fn [v] (set-state! assoc :free-input v))}))
     (! :row
        {:className "example-field"}
        (! :field/text
           {:name "Text Field"
            :value (:textarea-field state)
            :onChange (fn [v] (set-state! assoc :textarea-field v))}))
     (! :row
        {:className "example-field"}
        (! :field/password
           {:name "Password Field"
            :value (:password state)
            :onChange (fn [v] (set-state! assoc :password v))}))
     (! :row
        {:className "example-field"}
        (! :field/boolean
           {:name "Boolean Field"
            :value (:boolean-field state)
            :onChange (fn [] (set-state! update :boolean-field not))}))
     (! :row
        {:className "example-field"}
        (! :field/integer
           {:name "Integer Field"
            :value (:integer-field state)
            :onChange (fn [v] (set-state! assoc :integer-field v))}))
     (! :row
        {:className "example-field"}
        (! :field/float
           {:name "Float Field"
            :value (:float-field state)
            :onChange (fn [v] (set-state! assoc :float-field v))})))))

(def test-options
  [{:name "one"}
   {:name "two"}
   {:name "three"}
   {:name "four" :context :positive}
   {:name "five" :context :negative}
   {:name "six"}
   {:name "sevent"}])

(defnc select-fields
  []
  (let [[state set-state!] (hooks/use-state {:multiselect-field []})]
    (<>
     (! :row
        {:className "example-field"}
        (! :field/dropdown
           {:name "dropdown"
            :value (:dropdown-field state)
            :search-fn :name
            :context-fn :context
            :options test-options
            :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
     (! :row
        {:className "example-field"}
        (! :field/multiselect
           {:name "Multiselect Field"
            :value (:multiselect-field state)
            :placeholder "Choose.."
            :search-fn :name
            :context-fn :context
            :options test-options
            :onRemove (fn [v] (set-state! assoc :multiselect-field v))
            :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
     (! :row
        {:className "example-field"}
        (! :field/identity
           {:name "Identity Field"
            :value (:identity-field state)
            :placeholder "Choose..."
            :options [{:name "John"}
                      {:name "Harry"}
                      {:name "Ivan"}]
            :onChange (fn [v] (set-state! assoc :identity-field v))}))
     (! :row
        {:className "example-field"}
        (! :field/identity-multiselect
           {:name "Identity Multiselect"
            :value (:identity-multiselect-field state)
            :placeholder "Select..."
            :options [{:name "John"}
                      {:name "Harry"}
                      {:name "Ivan"}
                      {:name "Kiki"}
                      {:name "Rita"}
                      {:name "Tia"}]
            :onRemove (fn [v] (set-state! assoc :identity-multiselect-field v))
            :onChange (fn [v] (set-state! assoc :identity-multiselect-field v))})))))

(defnc date-fields
  []
  (let [[state set-state!] (hooks/use-state nil)]
    (<>
     (! :row
        {:className "example-field"}
        (! :field/date
           {:name "Date Field"
            :value (:date-field state)
            :onChange #(set-state! assoc :date-field %)}))
     (! :row
        {:className "example-field"}
        (! :field/timestamp
           {:name "Timestamp Field"
            :value (:timestamp-field state)
            :onChange #(set-state! assoc :timestamp-field %)}))
     (! :row
        {:className "example-field"}
        (! :field/date-period
           {:name "Date Period Field"
            :value (:date-period-field state)
            :onChange (fn [v] (set-state! assoc :date-period-field v))}))
     (! :row
        {:className "example-field"}
        (! :field/timestamp-period
           {:name "Timestamp Period Field"
            :value (:timestamp-period-field state)
            :onChange (fn [v] (set-state! assoc :timestamp-period-field v))})))))

(defnc Inputs
  {:wrap [(router/wrap-rendered :toddler.inputs)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    (! :simplebar
       {:style {:height height
                :width width}}
       (! :row {:align :center}
          (! :column
             {:align :center
              :style {:max-width "30rem"}
              :className (css
                          ["& .example-field" :my-5])}
             ($ md/watch-url {:url "/inputs.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "buttons-example")}
                ($ buttons))
             ($ toddler/portal
                {:locator #(.getElementById js/document "value-fields-example")}
                ($ value-fields))
             ($ toddler/portal
                {:locator #(.getElementById js/document "select-fields-example")}
                ($ select-fields))
             ($ toddler/portal
                {:locator #(.getElementById js/document "date-fields-example")}
                ($ date-fields)))))))
