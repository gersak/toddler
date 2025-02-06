## In general
Tables in Toddler are focused on making it easy to handle three hard
problems. At least hard for me.

First problem is how can i control table **layout**? How can i control
column sizes. What will happen if parent container is resized and
table is overflowing. Or what happens when it is resized but table
is far smaller? Do I reposition it or do i scale it? How?


Second problem is **how do I distribute data** through table. There is no need
to change redraw whole table when single cell changes. So how do I distribute
data/props to children component so that every row and column can have relevant data.

Third problem is **customization**. Single case scenario solution for above
goals isn't hard. But how to make it easy to customize cells, and rows?
Sometimes rows can be expanded or rows can be drag and dropped. What then?
Copy paste or, component configuration or... Break the wheel?


## What we know?
 * There will certainly be static data or configuration
or props or whatever you wan't to call it that will define how table looks. Something like
column definition

 * Beside column definition, there is need to propagate actions and events to upstream component
that will hold state. Some kind of dispatch would be preferable to keep consistency 

 * Control upstream and hold state in some parent component. Keep in mind DRY

 * Custom components for cells and rows

 * Don't wan't to worry about scrolling and overflowing and resizing. **This is posible if I set
 table in container with known height and width so that table can adjust its body and header
 size and width to that container.**

 * Fast rendering. Elements grow exponentially multiplying
cells and rows. Every unnecessary render will be resource intensive. On cell change I
wan't to render "table" than "row" than single changed cell in "row". Nothing else.


## How?

Lets start with table definition or what table should render. Columns should
be sequence of column definitions that define:

 * **:cursor** - cell cursor, like if i get row data in cell how can i access 
 value that should be displayed in this column. 
 Valid values are evenrything that can be used in *clojure.core/get* **or** *clojure.core/get-in*
 * **:cell**   - component that should be used to display cell. This component 
 **will not receive any props**. It will use ```toddler.table/use-cell-state```
 hook to get and change data. This makes it easy to customize cells!
 * **:header** - same as *:cell*. It should render column header.
 Also receives 0 props but same as cell will have access to column context 
 through ```toddler.table/use-column``` hook. So everyting that you put into 
 column definition will be available to that header component
 * **:style**  - style to override cell wrapper
 * **:width**  - column width. For implementation **[flex](https://css-tricks.com/snippets/css/a-guide-to-flexbox/)**
 is used. This *width* property will determine minimal size of column in pixels. As width changes (like on resize),
 column will grow but it will never shrink on lower value than *width*. Also column size ratio is persistent.
 * **:align**  - how to align column and header. Allowed values include keywords 
 **:left, :right, :center** or combination of same with **:top, :bottom** 
 in form of set. I.E. ```#{:left :center}``` or ``` #{:bottom :right}```

This is enough to describe what should be displayed and in a sense even how
by specifying **:cell** and **:header** components.

Example below shows how to define columns and use default implementation 
of cells and table to render table that can be interacted with for every
cell and header (order by).


## DEMO
<div id="toddler-table-example"></div>


```clojure
(ns toddler.showcase.table
  (:require
   [vura.core :as vura]
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [toddler.ui :as ui]
   [toddler.layout :as layout]
   [toddler.table :as table]))


(def columns
  [{:cursor :euuid
    :label "UUID"
    :align :center
    :header nil
    :cell :cell/uuid
    :width 50}
   {:cursor :user
    :label "User"
    :cell :cell/identity
    :options (repeatedly 3 random-user)
    :width 100}
   {:cursor :float
    :cell :cell/float
    :label "Float"
    :width 100}
   {:cursor :integer
    :cell :cell/integer
    :label "Integer"
    :width 100}
   {:cursor :text
    :cell :cell/text
    :label "Text"
    :width 250}
   {:cursor :enum
    :label "ENUM"
    :cell :cell/enum
    :options [{:name "Dog"
               :value :dog}
              {:name "Cat"
               :value :cat}
              {:name "Horse"
               :value :horse}
              {:name "Hippopotamus"
               :value :hypo}]
    :placeholder "Choose your fav"
    :width 100}
   {:cursor :timestamp
    :cell :cell/timestamp
    :label "Timestamp"
    :show-time false
    :width 120}
   {:cursor :boolean
    :cell  :cell/boolean
    :align :center
    :label "Boolean"
    :width 50}])


(defn generate-column
  [{t :cell}]
  (let [now (-> (vura/date) vura/time->value)]
    (letfn [(rand-date
              []
              (->
               now
               (+ (* (rand-nth [1 -1])
                     (vura/hours (rand-int 1000)))
                  (vura/minutes (rand-int 60)))
               vura/value->time))]
      (condp = t
        ui/uuid-cell (random-uuid)
        ui/integer-cell (rand-int 10000)
        ui/float-cell (* (rand) 1000)
        ui/identity-cell (random-user)
        ui/currency-cell {:amount (vura/round-number (* 1000 (rand)) 0.25)
                          :currency (rand-nth ["EUR" "USD" "HRK"])}
        ui/enum-cell (rand-nth (get-in columns [7 :options]))
        ui/timestamp-cell (rand-date)
        ui/text-cell (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
        ui/boolean-cell (rand-nth [true false])
        nil))))

(defn generate-row
  []
  (reduce
   (fn [r {c :cursor :as column}]
     (assoc r c (generate-column column)))
   {}
   columns))

(defn generate-table
  [cnt]
  (loop [c cnt
         r []]
    (if (zero? c) r
        (recur (dec c) (conj r (assoc (generate-row) :idx (count r)))))))

(def data (generate-table 50))


(defn reducer
  [{:keys [data] :as state}
    ;;
   {:keys [type idx value]
    {:keys [cursor]
     cidx :idx} :column}]
  (letfn [(apply-filters
            [{:keys [rows columns] :as state}]
            (if-some [filters (not-empty
                               (keep
                                (fn [{f :filter c :cursor}]
                                  (when f
                                    (case c
                                      :timestamp
                                      (let [[from to] f]
                                        (fn [{t :timestamp}]
                                          (cond
                                            (every? some? [from to]) (<= from t to)
                                            (some? from) (<= from t)
                                            (some? to) (<= t to))))
                                      :enum (comp f :enum)
                                      (constantly true))))
                                columns))]
              (assoc state :data (filter (apply every-pred filters) rows))
              (assoc state :data rows)))]
    (let [cursor' (if (sequential? cursor) cursor
                      [cursor])]
      (->
       (case type
         :table.element/change
         (assoc-in state (into [:rows (:idx (nth data idx))] cursor') value)
         state)
            ;;
       apply-filters))))

(defnc table-example
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)
        [{:keys [data columns]} dispatch] (hooks/use-reducer
                                           reducer
                                           {:rows data
                                            :data data
                                            :columns columns})]
    ($ ui/row
       {:align :center}
       (provider
        {:context layout/*container-dimensions*
         :value {:width 600
                 :height 500}}
        (! :table
           {:rows data
            :columns columns
            :dispatch dispatch})))))
````
In ```toddler.table``` namespace there are few components that are reusable and
are focused to help you bridge customization. You are encuraged to use this components
when you want custom behaviour and/or custom styling.

#### Cell
Component is kind of wrapper for **:cell** key component defined in columns. Cell
component is here to adjust flex parameters, to compute width based on column definition
and to provide context to **:cell** component(that doesn't get props from parent but
from context)

Why? Because columns don't change often. So *Cell* component won't be rendered often.
Column **:cell** defined component will ```use-context-value``` hook that returns 
value and set function for that value. Both of those depend on **\*row\*** context.

#### Row
Responsible for grouping columns. It takes columns with ```use-columns``` hook. Than
provides context for **\*row-record\*** context. Finally renders flex wrapper around
cells and maps Cell component with received columns from hook.

If any children components are added to Row component they will be rendered also. This
is nice place to expand row or something like that. More in example below.


#### Header
Same as row, except it renders **:header** component if available, and if not
it will skip rendering. If header component is not available
in column definition than only empty cell is rendered with params that match 
columns width in body cells.

Also header is wrapped with (hidden) simplebar instance to make it possible to sync
horizontal scroll.

#### Body
Component wraps all body rows inside of single div and wraps that div
inside of simplebar. That is it... Doesn't take any props except
*class* and *className*


#### TableProvider
Usefull for avoiding defining providers for context **\*dispatch\*, \*columns\* and \*rows\***.
That is it mostly, except it uses ```use-table-defaults``` hook to triage input params like **:columns**
and set default styles if needed.

## Expand Example
Here is demonstration of how to customize row so that it can be expanded
and how to use custom cell to display expansion action. Demo will show
table of movie characters where each row has additional details that 
can be view when toggling expand cell.

This is data:
```clojure
(def expand-data
  [{:first-name "Donnie" :last-name "Darko" :gender :male
    :movie "Donnie Darko, 2001"
    :description "Played by Jake Gyllenhaal, Donnie is a troubled teenager experiencing time loops, apocalyptic visions, and cryptic messages from a man in a rabbit suit."}
   {:first-name "Ellen" :last-name "Rippley" :gender :female
    :movie "Alien (1979) & Aliens (1986)"
    :description "Played by Guy Pearce, Leonard suffers from short-term memory loss and relies on tattoos and notes to track his search for his wife’s killer"}
   {:first-name "Sarah" :last-name "Connor" :gender :female
    :movie "The Terminator (1984) & Terminator 2: Judgment Day (1991)"
    :description "Played by Linda Hamilton, Sarah evolves from a terrified waitress to a hardened warrior, fighting to protect her son and prevent Judgment Day."}
   {:first-name "Leonard" :last-name "Shelby" :gender :male
    :movie "Memento, 2000"
    :description "Played by Guy Pearce, Leonard suffers from short-term memory loss and relies on tattoos and notes to track his search for his wife’s killer."}
   {:first-name "Roy" :last-name "Batty" :gender :male
    :movie "Blade Runner, 1982"
    :description "Played by Rutger Hauer, Roy is a rogue replicant searching for more life, delivering one of sci-fi’s most poetic monologues before his tragic end."}
   {:first-name "Furiosa" :gender :female
    :movie "Mad Max: Fury Road (2015)"
    :description "Played by Charlize Theron, Furiosa is a fearless, battle-hardened warrior fighting for freedom in a dystopian wasteland."}])
```

#### Expand cell
Lets start with expand cell. This is simple component that will show
icon down if it is not expanded and on click it will change its value
with *not* function effectively toggling on every click.

```clojure
(defnc expand-cell
  []
  (let [[value set-value!] (table/use-cell-state)]
    (d/div
     {:className (css :flex :flex-grow :items-center :justify-center :cursor-pointer)
      :on-click #(set-value! (not value))}
     ($ (if value outlined/keyboard-arrow-up outlined/keyboard-arrow-down)
        {:className (css {:font-size "24px"})}))))
```

#### Character cell
This cell will just concatenate first-name and last-name to display 
movie character.

```clojure
(defnc custom-cell
  []
  (let [{:keys [first-name last-name]} (table/use-row)]
    (d/div (str first-name " " last-name))))
```

<div id="expand-row-example"></div>

#### Custom row
Reusable ```toddler.table/Row``` component is used and passed ```extended-row```
component as child. ```extended-row``` component will use row context, that is will
get all information from row and from there pull values for **:movie, :description**
keys.

Also value of cursor ```[:ui :expanded]``` is monitored and if row isn't expanded than
height of extended-row is 0px and if it is it will match full size of that content.

*height* is transitioned so that extending row is smooth.

```clojure
(defnc extended-row
  []
  (let [{{:keys [expanded]} :ui
         :keys [movie description]} (table/use-row)
        [el {:keys [height]}] (toddler/use-dimensions)]
    (d/div
     {:className (css
                  :overflow-hidden
                  :text-xs
                  {:transition "height .2s ease-in-out"}
                  ["& label" :font-semibold :color+ :ml-4 {:min-width "100px"}])
      :style {:height (if expanded height 0)}}
     (d/div
      {:ref #(reset! el %)}
      ($ ui/row
         (d/label "Movie")
         (d/div movie))
      ($ ui/row
         (d/label "Description")
         (d/div description))))))

(defnc custom-row
  {:wrap [(ui/forward-ref)]}
  [props _ref]
  ($ table/Row
     {:ref _ref
      :className "trow"
      & (dissoc props :className :class)}
     ($ extended-row)))
```
#### Overriding default table row
Here you can see how columns are defined for this example. In addition
there is ```extend-ui``` wrapper used in ```row-example``` component.

This wrapper can be used to override component from inherited toddler
component context. Hence we are replacing default implementation of ```:table/row```
component and using ```custom-row``` from code above.

```clojure
(def row-example-columns
  [{:cursor [:ui :expanded]
    :cell expand-cell
    :width 32
    :align #{:center}
    :style {:max-width 48}}
   {:cell custom-cell
    :width 140
    :align #{:center :left}
    :label "Character"
    :header ui/plain-header}])

(defnc row-example
  {:wrap [(ui/forward-ref)
          (provider/extend-ui
           #:table {:row custom-row})]}
  []
  (let [[state set-state!] (hooks/use-state expand-data)]
    ($ ui/row
       {:align :center}
       ($ layout/Container
          {:style
           {:width 500
            :height 400}}
          ($ ui/table
             {:columns row-example-columns
              :dispatch (fn [{:keys [type value idx] :as evt
                              {:keys [cursor]} :column}]
                          (case type
                            :table.element/change (set-state! assoc-in (concat [idx] cursor) value)
                            (.error js/console "Unkown event: " (pr-str evt))))
              :rows state})))))
```

## DND Example
TBD
