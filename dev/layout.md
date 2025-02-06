

## Core

Throughout my years in frontend development, I've often struggled 
with issues related to overflowing content and scrolling. While it's 
possible to manage these challenges using a combination of HTML, CSS,
and `@media` rules, this often feels like building a house of 
cards—fragile and prone to breaking.

Every time I fixed one issue, another would arise. The most reliable 
approach I've found is to combine CSS with JavaScript and enforce
"fixed" width and height constraints on components. This is why,
in Toddler’s default components, you'll often find [Simplebar](https://grsmto.github.io/simplebar/)
being used to manage content containers, ensuring consistent
scrollbars across different browsers and operating systems.

#### container-dimensions

The `toddler.layout` namespace includes an essential context variable:
`*container-dimensions*`. This context allows child components to
compute how much space they can occupy within their parent container.

For example, consider a **tabs** component that contains multiple tab
content bindings. The component should display available tabs and their
respective content areas. Using `*container-dimensions*`, the tabs 
component would:

 * Retrieve the available container dimensions.
 * Render the tab headers.
 * Compute the remaining height after rendering the tab headers.
 * Pass the adjusted `*container-dimensions*` (with the reduced height)
to the child components, ensuring they fit within the allocated space.

Many other components follow this approach—leveraging `*container-dimensions*`
to manage space allocation dynamically. This value is often
passed to the Simplebar component to handle scrolling automatically.

#### Structured UI Layout

Typically, an **App** starts by capturing the **window dimensions**,
which are then subdivided into key UI sections such as the
navigation menu, action bar, and content area. By applying simple
mathematical operations on these main sections, developers gain 
greater control and flexibility over UI development.

#### Best Practice: Avoid Body Scroll

Instead of relying on the default **body scroll**, 
divide the viewport into structured **HUD-like components**
and use `*container-dimensions*` to refine space
allocation at a granular level. This method leads to
a more predictable and manageable UI layout.


## Rows and Columns
Row and column components are working on [flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/)
layout properties. Both will try to fill available space and grow as much as possible.

Both `column` and `row` accept `:position` or `:align` props value with one of 4 available
options:

 * **:start**   - start displaying elements from start
 * **:center**  - group elements at center 
 * **:end**     - group elements at the end of row/column
 * **:explode** - move elements away from each other as much as possible


By combining rows/columns with alignment and other css properties it is
not that hard to create __complex__ but reliable layout.

```clojure
($ ui/row
  {:align :center
   :className (css
               :bg-normal+ :border :border-normal
               :mt-4
               {:border-radius "50px"}
               ["& .element"
                :bg-normal- :border :border-normal+
                {:min-height "50px"
                 :max-width "50px"
                 :border-radius "50px"}])}
  ($ ui/column {:className "element"})
  ($ ui/column {:className "element"})
  ($ ui/column {:className "element"}))
```

<div id="rows-columns-example"></div>


#### List Example
This example displays user list. Following code is demonstrating
how to use `row` and `column` to create layout for user item component.


```clojure
(defnc user-item
  [{:keys [id name username email role bio]}]
  ($ ui/row
     {:className (css
                  :p-4
                  ["& .details img" :mt-3]
                  ["& .card" :p-2 :bg-normal+ :border :border-normal :rounded-md])}
     ($ ui/row
        {:className "card"
         :align :explode}
        ($ ui/row
           {:className "details"}
           ($ ui/avatar
              {:name name
               :size 48})
           ($ ui/column
              {:className (css
                           :ml-4
                           ["& .name" :font-semibold :text-sm]
                           ["& .info" :text-xxs :color :font-semibold]
                           ["& label" :color :font-normal :inline-block :mr-2 :text-xxxs])}
              (d/div {:className "name"} name)
              (d/div {:className "info"} (d/label "Username:") username)
              (d/div {:className "info"} (d/label "Email:") email)
              (d/div {:className "info"} (d/label "Role:") role)))
        ($ ui/column
           {:className (css
                        :self-start
                        :mt-1
                        {:max-width "200px"}
                        ["& label" :text-xxs :font-semibold :color-]
                        ["& p" :text-xxs])}
           (d/label "BIO")
           (d/p bio)))))
```

<div id="list-example"></div>



## TABS
Toddler default tabs are implemented with two components that are interdependant.

It works like this. There is `tabs` component that is container for all `tab` components
and it doesn't know how many tabs will be there. `tab` components are added as children to
`tabs` component and during that process `tab` component **registers** its :id, :name and 
:order (signup for position).

`tabs` component will then know what to display and it draws tabs and provides `*tabs*` context
to children. Children `tabs` can then check if its id is selected and if it is draw its content.

More details about tab(s) implementation can be found at [API]()


#### TRY CHANGING TAB
<div id="tabs-example"></div>

```clojure
(defnc user-list-tab
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)]
    ($ ui/tab
       {:id ::user-list
        :name "Users"}
       ($ ui/row
          {:align :center}
          ($ ui/simplebar
             {:style {:width width
                      :height height}}
             (map
              (fn [{:keys [id] :as data}]
                ($ user-item {:key id & data}))
              user-data))))))

(defnc table-tab
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/tab {:id ::table
               :name "Table"}
       ($ ui/table {:rows showcase.table/data
                    :columns showcase.table/columns}))))

(defnc tabs-example
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)]
    (provider
     {:context layout/*container-dimensions*
      :value {:width width
              :height (min height 400)}}
     ($ ui/tabs
        {:className (css :mt-2)}
        ($ user-list-tab)
        ($ table-tab)
        ($ showcase.modal/form-tab)))))
```

## GRID
Toddler grid hooks and components are based on implementation
of awesome [React-Grid-Layout](https://github.com/react-grid-layout/react-grid-layout) system.

Only basic features are covered, so no drag and drop or container resizing. Just rendering
grid based on grid props.

```use-grid-data``` in ```toddler.layout``` provides props for rendering grid
elements based on following props:

   * **layouts** - map that has layout id bound to sequence of grid
                   container definitions
   * **breakpoints** - map that has layout ids bound to width thresholds
                   so that hook can compute what layout is currently active
   * **columns** - number that will split available space on *#* columns
   * **row-height** - number that specifies single row height
   * **width**   - how much space is available for layout. this hook
                   will use that width to compute what layout from
                   provided layouts is active

<div id="grid-example"></div>

```clojure
(defnc grid-example
  []
  (let [[[width height] set-dimensions!] (hooks/use-state [400 400])
        columns {:lg 4 :md 3 :sm 1}
        breakpoints {:sm 100
                     :md 399
                     :lg 500}
        layouts {:sm [{:i "one" :x 0 :y 0 :w 1 :h 1}
                      {:i "two" :x 0 :y 1 :w 1 :h 1}
                      {:i "three" :x 0 :y 2 :w 1 :h 1}
                      {:i "four" :x 0 :y 3 :w 1 :h 1}]
                 :md [{:i "one" :x 1 :y 0 :w 2 :h 1}
                      {:i "two" :x 0 :y 1 :w 3 :h 1}
                      {:i "three" :x 0 :y 0 :w 1 :h 1}
                      {:i "four" :x 0 :y 2 :w 3 :h 2}]
                 :lg [{:i "one" :x 0 :y 0 :w 3 :h 1}
                      {:i "two" :x 3 :y 0 :w 1 :h 2}
                      {:i "three" :x 0 :y 1 :w 3 :h 1}
                      {:i "four" :x 0 :y 2 :w 4 :h 2}]}]
    (<>
     ($ ui/row
        {:align :center}
        ($ ui/row
           {:className (css :my-4 {:max-width "200px"})}
           ($ ui/dropdown-field
              {:value width
               :name "Choose width"
               :on-change #(set-dimensions! assoc 0 %)
               :options [300 400 600]})))
     ($ ui/row
        {:align :center}
        (let [$box (css :flex :w-full :h-full
                        :justify-center :items-center
                        :font-bold :text-xl)]
          ($ ui/grid
             {:width width
              :height height
              :margin [5 5]
              :columns columns
              :layouts layouts
              :breakpoints breakpoints
              :row-height 100}
             (d/div {:key "one" :class [(css :bg-red-500) $box]} "ONE")
             (d/div {:key "two" :class [(css :bg-green-500) $box]} "TWO")
             (d/div {:key "three" :class [(css :bg-yellow-500) $box]} "THREE")
             (d/div {:key "four" :class [(css :bg-cyan-500) $box]} "FOUR")))))))
```
