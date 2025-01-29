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

 * **:cursor** - cell cursor, like if i get row data in cell how can i access value that should be displayed in this column. Valid values are evenrything that can be used in *clojure.core/get* **or** *clojure.core/get-in*
 * **:cell**   - component that should be used to display cell. This component **will not receive any props**. It will use ```toddler.table/use-cell-state``` hook to get and change data. This makes it easy to customize cells!
 * **:header** - same as *:cell*. It should render column header. Also receives 0 props but same as cell will have access to column context through ```toddler.table/use-column``` hook. So everyting that you put into column definition will be available to that header component
 * **:style**  - style to override cell wrapper
 * **:width**  - column width. For implementation **[flex](https://css-tricks.com/snippets/css/a-guide-to-flexbox/)** is used. This *width* property will determine minimal size of column in pixels. As width changes (like on resize), column will grow but it will never shrink on lower value than *width*. Also column size ratio is persistent.
 * **:align**  - how to align column and header. Allowed values include keywords **:left, :right, :center** or combination of same with **:top, :bottom** in form of set. I.E. ```#{:left :center}``` or ``` #{:bottom :right}```

This is enough to describe what should be displayed and in a sense even how
by specifying **:cell** and **:header** components.

Example below shows how to define columns and use default implementation 
of cells and table to render table that can be interacted with for every
cell and header (order by).

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
  [{:cursor [:ui :expand]
    :cell ui/expand-cell
    :header nil
    :style {:width 20}}
   {:cursor :euuid
    :label "UUID"
    :align :center
    :header nil
    :cell ui/uuid-cell
    :style {:width 50}}
   {:cursor :user
    :label "User"
    :cell ui/identity-cell
    :options (repeatedly 3 random-user)
    :style {:width 100}}
   {:cursor :float
    :cell ui/float-cell
    :label "Float"
    :style {:width 100}}
   {:cursor :integer
    :cell ui/integer-cell
    :label "Integer"
    :style {:width 100}}
   {:cursor :text
    :cell ui/text-cell
    :label "Text"
    :style {:width 250}}
   {:cursor :currency
    :cell ui/currency-cell
    :style {:width 150}
    :label "Money"}
   {:cursor :enum
    :label "ENUM"
    :cell ui/enum-cell
    :options [{:name "Dog"
               :value :dog}
              {:name "Cat"
               :value :cat}
              {:name "Horse"
               :value :horse}
              {:name "Hippopotamus"
               :value :hypo}]
    :placeholder "Choose your fav"
    :style {:width 100}}
   {:cursor :timestamp
    :cell ui/timestamp-cell
    :label "Timestamp"
    :show-time false
    :style {:width 120}}
   {:cursor :boolean
    :cell ui/boolean-cell
    :label "Boolean"
    :type "boolean"
    :style {:width 50}}])


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
  (let [[{:keys [data columns]} dispatch] (hooks/use-reducer
                                           reducer
                                           {:rows data
                                            :data data
                                            :columns columns})]
    ($ layout/Container
       {:style {:width "480px"
                :height "500px"}}
       ($ ui/table
          {:rows data
           :columns columns
           :dispatch dispatch}))))
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
TBD

<div id="expand-row-example"></div>

## DND Example
TBD
