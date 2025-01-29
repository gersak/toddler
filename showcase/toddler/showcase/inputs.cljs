(ns toddler.showcase.inputs
  (:require
   [helix.core :refer [$ defnc <>]]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.core :as toddler]
   [toddler.md.lazy :as md]
   [toddler.layout :as layout]
   [toddler.core :refer [use-translate]]
   [toddler.router :as router]))

(defnc buttons
  []
  ($ ui/row
     {:align :center
      :class [(css :flex-wrap) "example-field"]}
     ($ ui/button "Default")
     ($ ui/button {:class "positive"} "Positive")
     ($ ui/button {:class "negative"} "Negative")
      ; ($ ui/button {:class "fun"} (translate :button.fun))
      ; ($ ui/button {:class "fresh"} (translate :button.fresh))
      ; ($ ui/button {:class "stale"} (translate :button.stale))
     ($ ui/button {:disabled true} "Disabled")))

(defnc value-fields
  []
  (let [[state set-state!]
        (hooks/use-state
         {:number-input 0
          :free-input ""
          :check-box false
          :integer-field 25000000
          :float-field 2.123543123123
          :textarea-field "I am text"})]
    (<>
     ($ ui/row
        {:className "example-field"}
        ($ ui/input-field
           {:name "Input Field"
            :value (:free-input state)
            :onChange (fn [v] (set-state! assoc :free-input v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/password-field
           {:name "Password Field"
            :value (:password state)
            :onChange (fn [v] (set-state! assoc :password v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/boolean-field
           {:name "Boolean Field"
            :value (:boolean-field state)
            :onChange (fn [] (set-state! update :boolean-field not))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/integer-field
           {:name "Integer Field"
            :value (:integer-field state)
            :onChange (fn [v] (set-state! assoc :integer-field v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/float-field
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
     ($ ui/row
        {:className "example-field"}
        ($ ui/dropdown-field
           {:name "dropdown"
            :value (:dropdown-field state)
            :search-fn :name
            :context-fn :context
            :options test-options
            :onChange (fn [v] (set-state! assoc :dropdown-field v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/multiselect-field
           {:name "Multiselect Field"
            :value (:multiselect-field state)
            :placeholder "Choose.."
            :search-fn :name
            :context-fn :context
            :options test-options
            :onRemove (fn [v] (set-state! assoc :multiselect-field v))
            :onChange (fn [v] (set-state! assoc :multiselect-field v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/identity-field
           {:name "Identity Field"
            :value (:identity-field state)
            :placeholder "Choose..."
            :options [{:name "John"}
                      {:name "Harry"}
                      {:name "Ivan"}]
            :onChange (fn [v] (set-state! assoc :identity-field v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/identity-multiselect-field
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
     ($ ui/row
        {:className "example-field"}
        ($ ui/date-field
           {:name "Date Field"
            :value (:date-field state)
            :onChange #(set-state! assoc :date-field %)}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/timestamp-field
           {:name "Timestamp Field"
            :value (:timestamp-field state)
            :onChange #(set-state! assoc :timestamp-field %)}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/date-period-field
           {:name "Date Period Field"
            :value (:date-period-field state)
            :onChange (fn [v] (set-state! assoc :date-period-field v))}))
     ($ ui/row
        {:className "example-field"}
        ($ ui/timestamp-period-field
           {:name "Timestamp Period Field"
            :value (:timestamp-period-field state)
            :onChange (fn [v] (set-state! assoc :timestamp-period-field v))})))))

(defnc Inputs
  {:wrap [(router/wrap-rendered :toddler.inputs)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
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
