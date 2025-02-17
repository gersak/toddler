(ns toddler.ui
  (:refer-clojure :exclude [identity])
  (:require-macros [toddler.ui :refer [defcomponent !]])
  (:require
   ["react" :as react]
   [helix.core
    :refer [create-context provider
            defnc $ fnc defhook]]
   [helix.children :as c]
   [helix.hooks :as hooks]))

(declare avatar row column form checkbox drawer tab tabs grid
         button buttons simplebar tooltip action tag ; card card-action card-actions
         ;; Calendar
         calendar-day calendar-month calendar calendar-period

         ;; Fields
         search-field identity-field identity-multiselect-field text-field integer-field
         float-field currency-field input-field dropdown-field multiselect-field timestamp-field
         timestamp-period-field date-field date-period-field boolean-field password-field
         idle-field copy-field

         ;; Table
         table table-cell table-row plain-header table-header-row
         ; enum-header currency-header boolean-header text-header user-header timestamp-header
         boolean-cell integer-cell float-cell text-cell enum-cell currency-cell hash-cell uuid-cell identity-cell
         timestamp-cell #_expand-cell #_delete-cell forward-ref modal-background modal-dialog)

(def ^{:js true
       :doc "Global context that holds mapping of keywords to
            react components. This context can be modified, extended
            or replaced for easier component management."}
  __components__
  (create-context))

(def forward-ref ^js react/forwardRef)

(defcomponent avatar :avatar
              "Component will render avatar image in form of <img> DOM element.
  Props should include:

   * :size `[:small, :medium, :large]` or override if number is specified
   * class(Name) - optional
   * style - optional")

(defcomponent row :row
              "Component will render **div** element that has display flex and
              will try to grow as much as it can. Items are aligned center
              and you can control how children will be positioned by specifying
              `:align` or `:position` prop with one of `[:start :end :center :explode]`")
(defcomponent column :column
              "Component will render **div** element that has display flex, flex-direction column, and
              will try to grow as much as it can. Items are aligned center
              and you can control how children will be positioned by specifying
              `:align` or `:position` prop with one of `[:start :end :center :explode]`")

(defcomponent form :form "NOT IMPLEMENTED YET")

(defcomponent checkbox :checkbox
              "Will render checkbox icon and inside `<div>` with all children components")

(defcomponent button :button
              "Renders single button. Button styling can be controled by specifying one of
  of following class(Name) `[\"positive\", \"negative\"]` or by specifying
  `:disabled` prop")

; (defcomponent buttons :buttons "")
(defcomponent simplebar :simplebar
              "Simplebar JS component. Expects `:width` and `:height` limit inside of
  `:style` prop to define scroll container.

  You can specify if you wan't simplebar to `show-shadow?` prop. If true
  simplebar will show shadow if content available but not visible.")

; (defcomponent img :img)
(defcomponent tag :tag)
; (defcomponent header :header)
; (defcomponent identity :identity)

(defcomponent tooltip :tooltip
              "Will create popup/Area div and will accept children components
  to fill that div element. When ever user hovers over popup/Area
  tooltip will be displayed.
  
  Content of tooltip will be `message` prop that can be text or
  `react-dom` element.
  
   * `preference` - prefered positioning sequence `[#{:top :left} #{:bottom :left}]`")

; (defcomponent field :field
;               "")
; (defcomponent input :input)
; (defcomponent textarea :textarea)

(defcomponent action :action
              "Component that will render `:icon` and `:name` props
  as. Other mentionalble props are

   * `class(Name)` - add CSS classes
   * `disabled`")

(defcomponent modal-background :modal/background
              "Props supported:

   * `class(Name)`
   * `:can-close?` - if click on background will call `on-close`
   * `:on-close`   - this callback will be called when
                     clicked on background")

(defcomponent modal-dialog :modal/dialog
              "Will create modal dialog on top of modal background. Children
  components will be rendered wrapped inside of modal dialog.
  
   * `:class(Name)`
   * `:on-close` - callback that will be called when dialog is closed
   * `:style     - element styling overrride`")

; (defcomponent card :card)

(defcomponent tabs :tabs
              "High level component. Will draw div.toddler-tabs with tab elements inside that div.
  `tabs` expects to be rendered with available layout/*container-dimensions* context.
  
  Children of `tabs` will be rendered inside div.tab-content. See [[tab]]")

(defcomponent tab :tab
              "Should be rendered as child of [[tabs]].

  Props:
   * `:name`     - Tab name, as what will be displayed
   * `:tab`      - same as name
   * `:id`       - unique id of tab.
   * `:focus?`   - if true will focus that tab on display
   * `:position` - prefered position in registered tabs
  ")

(defcomponent grid :grid
              "Grid component. Will split `:width` on number of columns
  defined in `:columns` by comparing current `:width` with defined
  breakpoints. In other words, finds highest breakpoint that fits
  width, takes that key, pulls from defined columns that key and
  gets number of columns.
  
   * `:row-height`  - defines height of single row
   * `:breakpoints` - mapping of keys to max width
   * `:columns`     - mapping of keys to number of columns
   * `:margin`      - spacing between grid elements
   * `:padding`     - padding inside of grid element
   * `:layouts`     - mapping of sequence of grid elements in folowing form:
  
  ```clojure
  {:sm [{:i \"one\" :x 0 :y 0 :w 1 :h 1}
        {:i \"two\" :x 0 :y 1 :w 1 :h 1}
        {:i \"three\" :x 0 :y 2 :w 1 :h 1}
        {:i \"four\" :x 0 :y 3 :w 1 :h 1}]
   :md [{:i \"one\" :x 1 :y 0 :w 2 :h 1}
        {:i \"two\" :x 0 :y 1 :w 3 :h 1}
        {:i \"three\" :x 0 :y 0 :w 1 :h 1}
        {:i \"four\" :x 0 :y 2 :w 3 :h 2}]
   :lg [{:i \"one\" :x 0 :y 0 :w 3 :h 1}
        {:i \"two\" :x 3 :y 0 :w 1 :h 2}
        {:i \"three\" :x 0 :y 1 :w 3 :h 1}
        {:i \"four\" :x 0 :y 2 :w 4 :h 2}]}
  ```

   * `:i` is ID
   * `:x` - start of grid element
   * `:y` - start of grid element
   * `:w` - width in columns
   * `:h` - height in rows")

(defcomponent calendar-day :calendar/day
              "Component that will render calendar day. Expects
  `vura.core/date-context` with additional props:
  
   * `:class(Name)`
   * `:period-start` - true if this day is period start
   * `:period-end`   - true if this day is period end
   * `:picked`       - if this only this day is selected
   * `:selected`     - in case of period, days :selected will be true
   * `:weekend?`     - true if this day is weekend")

(defcomponent calendar-month :calendar/month
              "Component will render calendar month. It expects

   * `:days` - that it will render. Use `toddler.date/use-calendar-month` hook
               to get days of month to display
   * `:on-select` - when day is selected handler 
   * `:disable`
   * `:read-only`")

(defcomponent calendar :calendar
              "Component will render calendar-month with calendar header
  where you can navigate year and month to display current
  `:value`.
  
  Use `on-change` callback to handle calendar changes.")

(defcomponent calendar-period :calendar/period
              "Component will render calendar-month with calendar header
  where you can navigate year and month to display current
  `:value`.
              
  Period value should be in form of vector `[start end]`.
  Values like `[start nil]` `[nil end]` are valid as well.
  
  Use `on-change` callback to handle calendar changes.")

;; Fields
(defcomponent search-field :field/search
              "Will track user input and on idle, when user stops
   typing, component will call `on-change` callback.

   * `:value`
   * `:on-change`
   * `:disabled`
   * `:placeholder`
  ")

(defcomponent identity-field :field/identity
              "Render identity field that will display avatar and
  result of applying `:search-fn` to `:value`

   * `:name`      - Name of the field
   * `:value`     - value of identity-field
   * `:placeholder` - value of field placeholder
   * `:opened`    - initial value of opened dropdown
   * `:options`   - available identity options
   * `:search-fn` - function that is used to search options
   * `:new-fn`    - when supplied value of input will create
                    value that is not one of provided `:options`")

(defcomponent identity-multiselect-field :field/identity-multiselect
              "Render identity multiselect field that will display avatar and
  result of applying `:search-fn` to `:value` and display selected values
  as tags.
              
   Props:

   * `:name`      - Name of the field
   * `:value`     - value of identity-field
   * `:placeholder` - value of field placeholder
   * `:opened`    - initial value of opened dropdown
   * `:options`   - available identity options
   * `:search-fn` - function that is used to search options
   * `:new-fn`    - when supplied value of input will create
                    value that is not one of provided `:options`")

(defcomponent text-field :field/text
              "Renders field that accepts following props:
  
  * `:name`        - name of field
  * `:value`
  * `:placeholder`
  * `:disabled`
  * `:read-only`
  * `:class(Name)  - will be conjoined to div.toddler-field element
  * `:on-change`   - callback that will receive value of textarea
                     as string
  
  **All other props are forwarded directly to `<textarea>` element**")

(defcomponent integer-field :field/integer
              "Renders field that accepts following props:
  
  * `:name`        - name of field
  * `:value`
  * `:placeholder`
  * `:disabled`
  * `:read-only`
  * `:class(Name)  - will be conjoined to div.toddler-field element
  * `:on-change`   - callback that will receive value of textarea
                     as string

  Value is formated based on locale when field is not active!
  
  **All other props are forwarded directly to `<input>` element**")

(defcomponent float-field :field/float
              "Renders field that accepts following props:
  
  * `:name`        - name of field
  * `:value`
  * `:placeholder`
  * `:disabled`
  * `:read-only`
  * `:class(Name)  - will be conjoined to div.toddler-field element
  * `:on-change`   - callback that will receive value of textarea
                     as string

  Value is formated based on locale when field is not active!
  
  **All other props are forwarded directly to `<input>` element**")

(defcomponent currency-field :field/currency)

(defcomponent input-field :field/input
              "Renders input field that accepts following props:
  
  * `:name`        - name of field
  * `:value`
  * `:placeholder`
  * `:disabled`
  * `:read-only`
  * `:class(Name)`  - will be conjoined to div.toddler-field element
  * `:on-change`   - callback that will receive value of textarea
                     as string
  
  **All other props are forwarded directly to `<input>` element**")

(defcomponent password-field :field/password
              "Renders input passsword field that accepts following props:
  
  * `:name`        - name of field
  * `:value`
  * `:placeholder`
  * `:disabled`
  * `:read-only`
  * `:class(Name)`  - will be conjoined to div.toddler-field element
  * `:on-change`   - callback that will receive value of textarea
                     as string
  
  **All other props are forwarded directly to `<input>` element**")

(defcomponent dropdown-field :field/dropdown
              "Render dropdown field that will display
  result of applying `:search-fn` to `:value`

   * `:name`      - Name of the field
   * `:value`     - value of identity-field
   * `:placeholder` - value of field placeholder
   * `:opened`    - initial value of opened dropdown
   * `:options`   - available identity options
   * `:search-fn` - function that is used to search options
   * `:new-fn`    - when supplied value of input will create
                    value that is not one of provided `:options`")

(defcomponent multiselect-field :field/multiselect
              "Render multiselect dropdown field that will display
  result of applying `:search-fn` to `:value`

   * `:name`      - Name of the field
   * `:value`     - value of identity-field
   * `:placeholder` - value of field placeholder
   * `:opened`    - initial value of opened dropdown
   * `:options`   - available identity options
   * `:search-fn` - function that is used to search options
   * `:new-fn`    - when supplied value of input will create
                    value that is not one of provided `:options`")

(defcomponent timestamp-field :field/timestamp
              "Render timestamp field that will display
  result of applying `:search-fn` to `:value`

   * `:name`      - Name of the field
   * `:value`     - value of identity-field
   * `:opened`    - initial value of opened dropdown
   * `:options`   - available identity options
   * `:format`    - one of `[:full-date
                             :long-date
                             :medium-date
                             :date
                             :full-time
                             :long-time
                             :medium-time
                             :time
                             :full-datetime
                             :long-datetime
                             :medium-datetime
                             :datetime
                             :calendar]`
   * `:time?`     - allow user to specify time
   * `:dropdown?` - open dropdown on user select or if
                    value is provided from other source
                    set this value to false")

(defcomponent timestamp-period-field :field/timestamp-period
              "Render timestamp period field that will display
  result of applying `:search-fn` to `:value`

   * `:name`      - Name of the field
   * `:value`     - value of identity-field
   * `:opened`    - initial value of opened dropdown
   * `:options`   - available identity options
   * `:format`    - one of `[:full-date
                             :long-date
                             :medium-date
                             :date
                             :full-time
                             :long-time
                             :medium-time
                             :time
                             :full-datetime
                             :long-datetime
                             :medium-datetime
                             :datetime
                             :calendar]`
   * `:time?`     - allow user to specify time
   * `:dropdown?` - open dropdown on user select or if
                    value is provided from other source
                    set this value to false")

(defcomponent date-field :field/date "Look at [[timestamp-field]]. This field overrides `:time?` value with `false`")

(defcomponent date-period-field :field/date-period "Look at [[timestamp-period-field]]. This field overrides `:time?` value with `false`")

(defcomponent boolean-field :field/boolean
              "Will display boolean field with \"checkbox\" and `:name` of field as label right to checkbox")

; (defcomponent checklist-field :field/checklist)

(defcomponent idle-field :field/idle
              "Renders input field that will call `on-change` when input is idle.
  
   * `:icon` - specify icon that should be rendered in input field
   * `:on-change` - callback that will be called with value of text
                    in `<input>`")

(defcomponent copy-field :field/copy
              "Renders field that will hold `:value` and when user click on
  this field it will copy value to clipboard")

;; Table
(defcomponent table :table
              "Component that will render table. It is rendered using flex layout that
  will display table header and table body as defined in table `:columns` prop.
  
  Header is renderd by rendering [[table-header-row]], and body rows are
  rendered by [[table-row]].
              
  Pass table data as **`:rows`** prop and define table `:columns` props as in example:
  
  ```
  [{:cursor :euuid
    :label \"UUID\"
    :align :center
    :header nil
    :cell :cell/uuid
    :width 50}
   {:cursor :user
    :label \"User\"
    :cell :cell/identity
    :options (repeatedly 3 random-user)
    :width 100}
   {:cursor :float
    :cell :cell/float
    :label \"Float\"
    :width 100}
   {:cursor :integer
    :cell :cell/integer
    :label \"Integer\"
    :width 100}
   {:cursor :text
    :cell :cell/text
    :label \"Text\"
    :width 250}
   {:cursor :enum
    :label \"ENUM\"
    :cell :cell/enum
    :options [{:name \"Dog\"
               :value :dog}
              {:name \"Cat\"
               :value :cat}
              {:name \"Horse\"
               :value :horse}
              {:name \"Hippopotamus\"
               :value :hypo}]
    :placeholder \"Choose your fav\"
    :width 100}
   {:cursor :timestamp
    :cell :cell/timestamp
    :label \"Timestamp\"
    :show-time false
    :width 120}
   {:cursor :boolean
    :cell  :cell/boolean
    :align :center
    :label \"Boolean\"
    :width 50}]
  ```")

(defcomponent table-cell :table/cell
              "When you pass `:columns` prop to table component it is
  mandatory to specify how to render `:cell` in table body
  and optional to specify how to render `:header` for table column
  header.
  
  Specified `:cell` prop will be used to render table cell.
  
  
  Use [[use-column]], [[use-row]] and [[use-cell-state]] to
  implement custom behaviour or use one of default implementations:
  
  ```
  [:cell/boolean
   :cell/integer
   :cell/float
   :cell/text
   :cell/enum
   :cell/uuid
   :cell/identity
   :cell/timestamp]
  ```
  
  For more details check out [table](https://gersak.github.io/toddler/tables)")

(defcomponent table-row :table/row
              "Will render `:cell` prop for every specified column that doesn't
              have `:hidden true` in column specification.")
(defcomponent table-header-row :table/header-row
              "Will render `:header` prop  for every specified column that doesn't
              have `:hidden true` in column specification.")

; (defcomponent enum-header :header/enum)
; (defcomponent currency-header :header/currency)
; (defcomponent boolean-header :header/boolean)
; (defcomponent text-header :header/text)
; (defcomponent user-header :header/user)
; (defcomponent timestamp-header :header/timestamp)
(defcomponent plain-header :header/plain)

(defcomponent boolean-cell :cell/boolean "Default implementation of :cell/boolean")
(defcomponent integer-cell :cell/integer "Default implementation of :cell/integer")
(defcomponent float-cell :cell/float "Default implementation of :cell/float")
(defcomponent text-cell :cell/text "Default implementation of :cell/text")
(defcomponent enum-cell :cell/enum "Default implementation of :cell/enum")
(defcomponent currency-cell :cell/currency "Not implemented fully")
(defcomponent hash-cell :cell/hashed "Default implementation of :cell/hashed")
(defcomponent uuid-cell :cell/uuid "Default implementation of :cell/uuid")
(defcomponent identity-cell :cell/identity "Default implementation of :cell/identity")
(defcomponent timestamp-cell :cell/timestamp "Default implementation of :cell/timestamp")
; (defcomponent expand-cell :cell/expand)
; (defcomponent delete-cell :cell/delete)

(defhook ^:no-doc use-component [id]
  (get (hooks/use-context __components__) id))

(defnc UI
  "Provider of UI components. It will provide `toddler.ui/__components__`
  context that is expected to be map of keywords maped to component
  implementation.
  
  For list of target components check out toddler.ui namespace"
  [{:keys [components] :as props}]
  (provider
   {:context __components__
    :value components}
   (c/children props)))

(defn wrap-ui
  "Wrapper for UI component. UI is Toddler ui components
  provider. Provider expects map with bound components to
  matching keys in toddler.ui namespace"
  ([component components]
   (fnc UI [props]
     ($ UI {:components components}
        ($ component {:& props})))))

(defnc ExtendUI
  "Component that is extending `toddler.ui/__commponents__` context
  by merging components in props to existing value.

  New components will be available to children components"
  [{:keys [components] :as props}]
  (let [current (hooks/use-context __components__)]
    (provider
     {:context __components__
      :value (merge current components)}
     (c/children props))))

(defn extend-ui
  "Wrapper that will extend Toddler UI by merging new
  components onto currently available components."
  ([component components]
   (fnc ExtendUI [props]
     ($ ExtendUI {:components components}
        ($ component {:& props})))))
