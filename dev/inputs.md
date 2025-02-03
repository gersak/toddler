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
Select fields use popup to show user what options
are available to select. User can select single option with dropdown
field or multiple options with multiselect field.


<div id="select-fields-example"></div>


Dropdown and multiselect are implemented using hooks and elements from
`toddler.dropdown` and `toddler.multiselect` namespaces. For example in
toddler.dropdown namespace there is hook `use-dropdown` that abstracts
dropdown mechanincs and returns map of state and dropdown handlers.

In short, dropdown is split into 3 parts. First are options,
second is input (how to display option or create new value)
and third is actual value.

Input(search) is transforming option value based on `:search-fn` from props.
Like if you have options in form of map or some other object,
than search-fn will be applied to that option.
Search fn should return string representation.

If `new-fn` is passed through props, than dropdown/multiselect can create new
value(s). `new-fn` will be applied to value of input on every input change and
`:on-change` handler received in props will be called with result value.

Options are displayed in dropdown popup same way as input value. Using `search-fn`.
`:on-change` handler received in props will be called on every option select.


## Date fields
Date fields use [calendar](../calendar) so that user can pick date
from calendar that is displayed in popup.

In this showcase there are two types of date fields. First one is
date or timestamp field that will hold single value. That is value of 
date and time that user selects.

Second is date or timestamp period field. User can select interval
between start of period and end of period or just select end or start
of period. In case where value of field doesn't contain start or end
calendar will display all dates before end or after start as selected.


Sometimes working with period field might seem awkward, so remember
that you can clear start and end input fields, as well as double click
on some date to focus start and end of that day.

<div id="date-fields-example"></div>

```clojure
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
```
