Toddler offers few components that tackle usual date selection
expirence. In examples bellow you will find code that demonstrates
how to use toddler.ui calendar components.

Default implementation for calendar components can be found in
```toddler.ui.elements.calendar``` namespace. Most components
that are implemented use hooks from ```toddler.date``` namespace.

```toddler.date``` namespace has most of "hard" stuff solved. It
contains implementation for calendar state. ```toddler.ui.elements.calendar```
is mostly visual representation.

## Calendar Month
Calendar month is component that will render calendar with month frame. To render
calendar-month component properly you will feed ```ui/calendar-month``` component with 
days that will be displayed.


This isn't easy since calendars visualy start with monday
and months don't. Situation will require some days from previous
month as well as some days from next month.

```toddler.date/use-calendar-month``` hook is implementation that
creates react reducer based on ```:date``` or ```:value``` prop.
Reducer will have :days key in its state and this value is feed 
that calendar-month is expecting.

<div id="calendar-month-example"></div>

```clojure
(defnc month
  []
  (let [[{:keys [days]}] (date/use-calendar-month {:date (js/Date.)})]
    ($ ui/row
       {:align :center}
       ($ ui/calendar-month
          {:days days}))))
```



## Calendar
Calendar component builds on top of *calendar-month* component. Difference is that
calendar adds header that can be used to navigate calendar month.

Calendar accepts ```:value``` prop and supports ```on-change``` handler
that will be triggered when user picks date.

<div id="calendar-example"></div>


```clojure
(defnc calendar
  []
  (let [[value set-value!] (hooks/use-state (js/Date.))]
    ($ ui/row
       {:align :center
        :className "component"}
       ($ ui/calendar
          {:value value
           :on-change set-value!}))))
```


## Calendar Period
Similar to calendar, calendar period has same navigation and expects
```:value``` to be vector of two elements **[start, end]**.


<div id="calendar-period-example"></div>


```clojure
(defnc calendar-period
  []
  (let [[value set-value!] (hooks/use-state nil)]
    ($ ui/row
       {:align :center
        :className "component"}
       ($ ui/calendar-period
          {:value value
           :on-change set-value!}))))
```
