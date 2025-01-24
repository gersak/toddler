

## Buttons
What is there to say about buttons. Not much, except that you can
give them flavor by adding class.

Default implementation is styled to support following classes:

 * positive
 * negative

There are plans about adding more classes, just haven't decided
about naming.

<div id="buttons-example"></div>

```clojure
(defnc buttons
  []
  ($ ui/row
     {:align :center
      :className (css :flex-wrap)}
     ($ ui/button "Default")
     ($ ui/button {:class "positive"} "Positive")
     ($ ui/button {:class "negative"} "Negative")
     ($ ui/button {:disabled true} "Disabled")))
```

## Scalar Fields
Scalar fields are refering to fields that expect scalar
values typed in by keybord or toggled by user action.

This type of field is simple, doesn't have any popup
elements or moving parts.

<div id="value-fields-example"></div>

```clojure
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
```

## Select fields

<div id="select-fields-example"></div>

## Date fields
<div id="date-fields-example"></div>
