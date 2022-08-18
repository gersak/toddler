(ns toddler.interactions
  (:require
   clojure.set
   clojure.string
   [goog.string :as gstr]
   [vura.core :as vura]
   [cljs-bean.core :refer [->js ->clj]]
   [helix.styled-components :refer [defstyled --themed]]
   [helix.core
    :refer [$ defnc provider
            memo defhook
            create-context]]
   [helix.dom :as d]
   [helix.children :as c]
   [helix.hooks  :as hooks]
   [helix.spring :as spring]
   [toddler.hooks
    :refer [use-translate
            use-calendar
            use-idle]]
   [toddler.elements.input
    :refer [AutosizeInput
            NumberInput
            IdleInput
            TextAreaElement
            SliderElement]]
   [toddler.elements.mask :refer [use-mask]]
   [toddler.elements.dropdown :as dropdown]
   [toddler.elements.multiselect :as multiselect]
   [toddler.elements.table :as table]
   [toddler.elements.popup :as popup]
   [toddler.elements.tooltip :as tip]
    ;;
   ["react" :as react]
   ["simplebar-react" :default SimpleBar]
   ["simplebar/dist/simplebar.min.css"]
   ["@fortawesome/react-fontawesome" :refer [FontAwesomeIcon]]
   ["@fortawesome/free-solid-svg-icons"
    :refer [faCheck
            faTimes
            faSearch
            faChevronDown
            faQuoteRight
            faMinus
            faAngleRight
            faAngleLeft
            faAngleDoubleLeft
            faAngleDoubleRight]]))


(defstyled simplebar SimpleBar
  {"transition" "box-shadow 0.3s ease-in-out"}
  --themed)

;; Basic components

(defstyled fa FontAwesomeIcon
  {:use-select "none"
   :cursor "pointer"
   :path {:cursor "pointer"}})


(defn --flex-position
  [{:keys [position]}]
  (when-some [jc (case position
                   :center "center"
                   :end "flex-end"
                   :explode "space-between"
                   nil)]
    {:justify-content jc
     ".wrapper" {:justify-content jc}}))

;; Datepicker
(defnc Column
  [{:keys [label style className] :as props}]
  (spring/div
   {:className className
    :style style}
   (when label
     (d/div
      {:className "label"}
      (d/label label)))
   (c/children props)))

(defstyled column Column
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"}
  --themed
  --flex-position)

;;

(defnc Row
  [{:keys [label className style] :as props}]
  (spring/div
   {:className className
    :style style}
   (when label
     (d/div
      {:className "label"}
      (d/label label)))
   (c/children props)))

(defstyled row Row
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :flex-grow "1"}
  --themed
  --flex-position)

;;

(defnc Info
  [{:keys [text] :as props}]
  (d/div
   {& (dissoc props :text)}
   ($ FontAwesomeIcon {:icon faQuoteRight :pull "left"})
   (d/p text)))

;;

(defstyled info Info
  {:display "flex"
   :align-items "baseline"
   :p {:margin "5px 0"}})


(def action-tooltip tip/action-tooltip)


(defnc Action
  [{:keys [tooltip disabled]
    icon :icon
    :as props}]
  (let [[style api] (spring/use-spring (fn [] {:transform "scale(1)"}))]
    ($ action-tooltip
       {:message tooltip
        :disabled (or (empty? tooltip) disabled)}
       (spring/div
        {:style style
         :onMouseDown (fn [] (api :start {:transform "scale(0.6)" :config {:tension 2000}}))
         :onMouseUp (fn [] (api :start {:transform "scale(1)" :config {:delay 200 :tension 2000}}))
         :onMouseEnter #(api :start {:transform "scale(1.2)" :config {:tension 2000}})
         :onMouseLeave #(api :start {:transform "scale(1)" :config {:tension 2000}})
         :& (dissoc props :tooltip :icon :icon-position)}
        (when icon ($ fa {:icon icon :className "icon"}))
        (c/children props)))))


(def $action
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :padding 3
   :use-select "none"
   :cursor :pointer
   :font-size "12"
   :transition "all .2s linear"
   :path {:cursor "pointer"}
   :user-select "none"
   :margin "0px 5px"
   :min-height 36
   :svg {:margin "0 3px"}
   ":hover"
   {:opacity "1"}})


(defstyled action Action
  $action
  --themed)


(defstyled named-action
  "div"
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :padding 3
   :use-select "none"
   :cursor :pointer
   :opacity ".7"
   :font-size "14"
   :transition "all .2s linear"
   :path {:cursor "pointer"}
   :user-select "none"
   :margin "7px 4px"
   :svg {:margin "0 3px"}
   ":hover"
   {:opacity "1"
    :font-size "16"}}
  (fn [{:keys [icon-position]}]
    (case icon-position
      :right {:svg {:margin-left 5}}
      {:svg {:margin-right 5}}))
  --themed)



(defstyled buttons
  "div"
  {:display "flex"
   :button {:margin 0
            :border-radius 0}
   :button:first-of-type {:border-top-left-radius 4 :border-bottom-left-radius 4}
   :button:last-of-type {:border-top-right-radius 4 :border-bottom-right-radius 4}})


(defstyled button
  "button"
  {:border "2px solid transparent"
   :border-radius 2
   :padding "5px 18px"
   :max-height 30
   :min-width 80
   :font-size "12"
   :line-height "1.33"
   :text-align "center"
   :vertical-align "center"
   :transition "box-shadow .3s ease-in,background .3s ease-in"
   :cursor "pointer"
   :margin "3px 2px"
   ":hover" {:transition "background .3s ease-in"}
   ":focus" {:outline "none"}
   ":active" {:transform "translate(0px,2px)" :box-shadow "none"}}
  --themed)


(defstyled checkbox-button
  "button"
  {:cursor "pointer"
   :path {:cursor "pointer"}
   :transition "color .2s ease-in"
   :width 20
   :height 20
   :border-radius 3
   :border-color "transparent"
   :padding 9
   :display "flex"
   :justify-content "center"
   :outline "none"
   :align-items "center"
   ":active" {:border-color "transparent"}}
  --themed)


(defnc checkbox [{:keys [active] :as props}]
  ($ checkbox-button
     {:$active active & (dissoc props :active)}
     ($ FontAwesomeIcon
        {:icon (case active
                 nil faMinus
                 faCheck)})))


(defnc CheckboxField [{:keys [name className] :as props}]
  (d/span
   {:class className}
   ($ checkbox {& (dissoc props :name :className)})
   (d/label
    {:className "field-name"}
    name)))


(defstyled checkbox-field CheckboxField
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :margin "5px 10px"
   ".field-name"
   {:margin-left 5
    :user-select "none"
    :transition "all .3s ease-in-out"
    :font-size "12"
    :font-weight "600"
    :text-transform "uppercase"}}
  --themed)


(defstyled interactions
  "div"
  {:display "flex"
   :flex-direction "row"
   :flex-wrap "wrap"
   :align-items "center"
   :padding "0 5px;"
   :min-height 40
   (str " " action)
   {:padding "3px"
    :margin "7px 2px"}}
  --themed
  --flex-position)


(defn --editable-tag [{:keys [editable?]}]
  (when-not editable?
    {:user-select "none"}))


(def $tag
  {:margin 3
   :display "flex"
   :flex-direction "row"
   :justify-content "start"
   :align-items "center"
   ; :flex-wrap "wrap"
   ".content"
   {:padding "5px 5px"
    :justify-content "center"
    :align-items "center"
    :font-size "12"
    :display "flex"}
   :svg {:margin "0 5px"
         :padding-right 3}
   :border-radius 3}
  --editable-tag)

(defnc DefaultTagContent
  [{:keys [value className]}]
  (d/div {:className className} value))

(defnc Tag
  [{:keys [value
           context
           on-remove
           onRemove
           disabled
           className
           render/content]
    :or {content DefaultTagContent}}]
  (let [on-remove (some #(when (fn? %) %) [onRemove on-remove])]
    (d/div
     {:context (if disabled :stale context)
      :className className}
     ($ content {:className "content" :value value})
     (when on-remove
       ($ FontAwesomeIcon
          {:icon faTimes
           :className "remove"
           :pull "left"
           :size "xs"
           :onClick (fn [] (on-remove value))})))))


(defstyled tag Tag
  $tag
  --themed)

(defstyled slider SliderElement
  {:-webkit-appearance "none"
   :appearance "none"
   :outline "none"
   :opacity "0.7"
   :padding 2
   :transition "opacity .2s"}
  --themed)


(defstyled autosize-input
  AutosizeInput
  {:outline "none"
   :border "none"}
  #_--themed)


(defstyled idle-input
  IdleInput
  {:outline "none"
   :border "none"}
  #_--themed)


(defstyled autosize-text TextAreaElement
  {:pre {:font-family "Ubuntu"}})

(defstyled number-input NumberInput
  {:outline "none"
   :border "none"})

(defnc Mask [props]
  (let [props' (use-mask props)]
    (d/div
     {:class "eywa-mask-field"}
     (d/input
      {:spellCheck false
       :auto-complete "off"
       & (dissoc props' :constraints :delimiters :mask)}))))


(defstyled mask-input Mask
  {:outline "none"
   :border "none"})



(defstyled dropdown-option
  "div"
  {:padding "0 12px 0 7px"
   :font-size "12"
   :display "flex"
   :justify-content "flex-start"
   :align-items "center"}
  --themed)

(defstyled dropdown-popup "div"
  {:display "flex"
   :flex-direction "column"
   :border-radius 3
   :padding 7}
  --themed)

(defstyled dropdown-wrapper
  "div"
  {:display "flex"
   :justify-content "row"
   :align-items "center"}
  --themed)


(def ^:dynamic ^js *dropdown* (create-context))
(def ^:dynamic ^js *dropdown-area* (create-context))

(defnc DropdownElementDecorator
  [{:keys [className]}]
  (let [{:keys [options opened disabled]} (hooks/use-context *dropdown*)]
    (when (and (not disabled) (pos? (count options)))
      (d/span
       {:className (str
                    className
                    (when opened " opened"))}
       ($ FontAwesomeIcon {:icon faChevronDown :pull "left" :size "xs"})))))

(defstyled dropdown-element-decorator DropdownElementDecorator
  {:position "absolute"
   :right 0
   :top 7
   :transition "color .2s ease-in-out"}
  --themed)

(defnc DropdownElementDiscard
  [{:keys [className]}]
  (let [{:keys [value discard!]} (hooks/use-context *dropdown*)]
    (when (some? value)
      (d/span
       {:className className
        :onClick discard!}
       ($ FontAwesomeIcon
          {:icon faTimes
           :pull "left"
           :size "xs"})))))


(defstyled dropdown-field-discard DropdownElementDiscard
  {:position "absolute"
   :right 0
   :top 6
   :transition "color .2s ease-in-out"}
  --themed)

(defnc DropdownArea
  [props]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        {:keys [area] :as dropdown}
        (dropdown/use-dropdown
         (->
          props
          (assoc :area-position area-position)
          (dissoc :className)))]
    (provider
     {:context *dropdown*
      :value dropdown}
     (provider
      {:context *dropdown-area*
       :value [area-position set-area-position!]}
      ($ popup/Area
         {:ref area
          & (select-keys props [:className])}
         (c/children props))))))


(defnc DropdownInput
  [{:keys [className onSearchChange]
    rinput :render/input
    rwrapper :render/wrapper
    rimg :render/img
    :or {rinput autosize-input
         rwrapper dropdown-wrapper}
    :as props}]
  (let [{:keys [input
                value
                search
                on-change
                on-key-down
                sync-search!
                toggle!
                opened
                placeholder
                disabled
                read-only
                searchable?]
         :or {searchable? true}} (hooks/use-context *dropdown*)]
    (hooks/use-effect
     [search]
     (when (ifn? onSearchChange)
       (onSearchChange search)))
    ($ rwrapper
       {:onClick toggle!
        :className (cond-> className
                     opened (str " opened"))
        & (select-keys props [:context :disabled])}
       (when rimg ($ rimg {& value}))
       ($ rinput
          {:ref input
           :className "input"
           :value search
           :read-only (or read-only (not searchable?))
           :disabled disabled
           :spellCheck false
           :auto-complete "off"
           :placeholder placeholder
           :onChange on-change
           :onBlur sync-search!
           :onKeyDown on-key-down})
       (c/children props))))

(defnc DropdownPopup
  [{:keys [className]
    rpopup :render/popup
    roption :render/option
    :or {rpopup dropdown-popup
         roption dropdown-option}}]
  (let [[area-position set-area-position!] (hooks/use-context *dropdown-area*)
        {:keys [options
                popup
                disabled
                opened
                read-only
                search-fn
                ref-fn
                cursor
                select!
                close!]
         :or {search-fn str}} (hooks/use-context *dropdown*)]
    (when (and (not read-only) (not disabled) (pos? (count options)) opened)
      ($ popup/Element
         {:ref popup
          :items options
          :onChange (fn [{:keys [position]}]
                      (when (some? position) (set-area-position! position)))
          :className (str className " animated fadeIn faster")
          :wrapper rpopup}
         (map
          (fn [option]
            ($ roption
               {:key (search-fn option)
                :ref (ref-fn option)
                :option option
                :selected (= option cursor)
                :onMouseDown (fn []
                               (select! option)
                               (close!))
                & (cond-> nil
                    (nil? option) (assoc :style #js {:color "transparent"}))}
               (if (nil? option) "nil" (search-fn option))))
          (if (:top area-position)
            (reverse options)
            options))))))


(defnc DropdownElement
  [{rinput :render/input
    rpopup :render/popup
    :or {rinput DropdownInput
         rpopup DropdownPopup}
    :as props}]
  ($ DropdownArea
     {& props}
     ($ rinput)
     ($ rpopup)))

;;

(defnc CurrencyElement
  [{:keys [currency amount
           currency/options placeholder
           className onChange
           onBlur on-blur onFocus on-focus]}]
  (let [on-blur (or onBlur on-blur identity)
        on-focus (or onFocus on-focus identity)
        [focused? set-focused!] (hooks/use-state nil)
        value (if (some? amount)
                (if (not focused?)
                  (str amount)
                  ; (format-currency currency amount)
                  (str amount))
                "")]
    (d/div
     {:className className}
     ($ DropdownElement
        {:options options
         :value currency
         :placeholder "VAL"
         :onChange (fn [currency]
                     (onChange {:currency currency
                                :amount amount}))
         :className "dropdown"})
     (d/input
      {:value value
       :autoComplete "off"
       :autoCorrect "off"
       :spellCheck "false"
       :autoCapitalize "false"
       :placeholder placeholder
       :disabled (nil? amount)
       :onChange (fn [e]
                   (some->>
                    (.. e -target -value)
                    not-empty
                    (re-find #"-?\d+[\.|,]*\d*")
                    (js/parseFloat)
                    onChange))
       :className "input"
       :onBlur (fn [e]
                 (set-focused! false)
                 (on-blur e))
         ;;
       :onFocus (fn [e]
                  (set-focused! true)
                  (on-focus e))}))))


;; CALENDAR

(def ^:dynamic ^js *calendar-events* (create-context))
(def ^:dynamic ^js *calendar-selected* (create-context))
(def ^:dynamic ^js *calendar-disabled* (create-context))
(def ^:dynamic ^js *calendar-control* (create-context))
(def ^:dynamic ^js *calendar-opened* (create-context))
(def ^:dynamic ^js *calendar-state* (create-context))


(defhook use-calendar-events [] (hooks/use-context *calendar-events*))
(defhook use-calendar-state [] (hooks/use-context *calendar-state*))


(defnc CalendarDay
  [{:keys [value
           day-in-month
           className] :as props}]
  (let [{on-select :on-day-change} (hooks/use-context *calendar-events*)
        is-disabled (hooks/use-context *calendar-disabled*)
        is-selected (hooks/use-context *calendar-selected*)
        disabled (when (ifn? is-disabled) (is-disabled props))
        selected (when (ifn? is-selected) (is-selected props))]
    (d/div
     {:class className}
     (d/div
      {:class (cond-> ["day"]
                selected (conj "selected")
                disabled (conj "disabled")
                (nil? value) (conj "empty"))
       :onClick (fn []
                  (when-not disabled
                    (when (fn? on-select)
                      (on-select day-in-month))))}
      (d/div (or day-in-month " "))))))


(defstyled calendar-day CalendarDay
  {:border-collapse "collapse"
   :border "1px solid transparent"
   ".day"
   {:text-align "center"
    :font-size "10"
    :user-select "none"
    :padding 3
    :width 20
    :border-collapse "collapse"
    :border "1px solid transparent"
    :cursor "pointer"
    ".empty" {:cursor "default"}}}
  --themed)


(def ^:dynamic ^js *calendar-day* (create-context calendar-day))


(defnc CalendarWeek [{:keys [days className]}]
  (let [days (group-by :day days)
        calendar-day (hooks/use-context *calendar-day*)]
    (d/div
     {:class className}
     (d/div
      {:class "week-days"}
      ($ calendar-day {:key 1 :day 1 & (get-in days [1 0] {})})
      ($ calendar-day {:key 2 :day 2 & (get-in days [2 0] {})})
      ($ calendar-day {:key 3 :day 3 & (get-in days [3 0] {})})
      ($ calendar-day {:key 4 :day 4 & (get-in days [4 0] {})})
      ($ calendar-day {:key 5 :day 5 & (get-in days [5 0] {})})
      ($ calendar-day {:key 6 :day 6 & (get-in days [6 0] {})})
      ($ calendar-day {:key 7 :day 7 & (get-in days [7 0] {})})))))

(defstyled calendar-week CalendarWeek
  {".week-days"
   {:display "flex"
    :flex-direction "row"}}
  --themed)


(def ^:dynamic *calendar-week* (create-context calendar-week))


(defnc CalendarMonthHeader
  [{:keys [className days]}]
  (let [week-days (use-calendar :weekdays-short)
        day-names (zipmap
                   [7 1 2 3 4 5 6]
                   week-days)]
    (d/div
     {:class className}
     (map
      (fn [n]
        (d/div
         {:class "day-wrapper"
          :key n}
         (d/div {:class "day"} (get day-names n))))
      days))))


(defstyled calendar-month-header CalendarMonthHeader
  {:display "flex"
   :flex-direction "row"
   :border-radius 3
   :cursor "default"
   ".day-wrapper"
   {:border-collapse "collapse"
    :border "1px solid transparent"
    ".day"
    {:text-align "center"
     :font-weight "500"
     :font-size "12"
     :border-collapse "collapse"
     :user-select "none"
     :padding 3
     :width 20
     :border "1px solid transparent"}}}
  --themed)


(def ^:dynamic *calendar-month-header* (create-context calendar-month-header))


(defnc CalendarMonth
  [{:keys [className days]}]
  (let [weeks (sort-by key (group-by :week days))
        month-header (hooks/use-context *calendar-month-header*)
        calendar-week (hooks/use-context *calendar-week*)]
    (d/div
     {:class className}
     ($ month-header {:days (range 1 8)})
     (map
      #($ calendar-week {:key (key %) :week (key %) :days (val %)})
      weeks))))


(defstyled calendar-month CalendarMonth
  {:display "flex"
   :flex-direction "column"
   :width 220}
  --themed)


(defnc CalendarMonthDropdown
  [{:keys [value placeholder className]
    :or {placeholder "-"}
    :as props}]
  (let [value (or value (vura/month? (vura/date)))
        {on-month-change :on-month-change} (use-calendar-events)
        months (range 1 13)
        month-names (use-calendar :months-long)
        search-fn (zipmap months month-names)
        props' (assoc props
                      :onChange on-month-change
                      :search-fn search-fn
                      :options months
                      :position-preference popup/central-preference
                      :value value)]
    ($ DropdownElement
       {:placeholder placeholder
        :className className
        & props'})))


(defstyled calendar-month-dropdown CalendarMonthDropdown
  {:margin "5px 0"
   :cursor "pointer"
   :input {:cursor "pointer"
           :color "red"}})


(defnc CalendarYearDropdown
  [{:keys [value placeholder className]
    :or {placeholder "-"}
    :as props}]
  (let [value (or value (vura/year? (vura/date)))
        {on-year-change :on-year-change} (use-calendar-events)
        props' (assoc props
                      :value value
                      :onChange on-year-change
                      :position-preference popup/central-preference
                      :options
                      (let [year (vura/year? (vura/date))]
                        (range (- year 5) (+ year 5))))]
    ;;
    ($ DropdownElement
       {:placeholder placeholder
        :className className
        & props'})))


(defstyled calendar-year-dropdown CalendarYearDropdown
  {:margin "5px 0"
   :cursor "pointer"
   :input {:cursor "pointer"
           :color "red"}})


;; Fields

(defstyled field-wrapper
  "div"
  {:border "1px solid"
   :border-radius 2
   :margin-top 4
   :padding "4px 10px"
   :cursor "text"
   :input {:font-size "12"}
   :overflow "hidden"}
  --themed)

(defnc Field
  [{:keys [name className style]
    :as props}]
  (d/div
   {:class className
    :style (->clj style)
    & (select-keys props [:onClick])}
   (when name (d/label {:className "field-name"} name))
   (c/children props)))

(defstyled default-field Field
  {:display "flex"
   :flex-direction "column"
   :margin "5px 10px"}
  --themed)


(defstyled field-column column
  {".field" {:flex-grow "1"}})

(defstyled field-row row
  {".field" {:flex-grow "1"}})

(defnc WrappedField
  [{:keys [context
           render/field
           render/wrapper]
    :or {wrapper field-wrapper
         field default-field}
    :as props}]
  ($ field {& props}
     ($ wrapper {:context context}
        (c/children props))))


(defnc InputField
  [{:keys [render/field render/input]
    :or {field WrappedField
         input autosize-input}
    :as props}]
  (let [_input (hooks/use-ref nil)]
    ($ field
       {:onClick (fn [] (when @_input (.focus @_input)))
        & props}
       ($ input
          {:ref _input
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           & (dissoc props :name :className :style :render/field :render/input)}))))


(defstyled input-field InputField nil --themed)


(defnc CurrencyField
  [{:keys [render/field]
    :or {field WrappedField}
    :as props}]
  {:wrap [(react/forwardRef)]}
  (let [input (hooks/use-ref nil)]
    ($ field
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ CurrencyElement
          {:ref #(reset! input %)
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           & (dissoc props :name :className :style)}))))

(defstyled currency-field CurrencyField nil --themed)

(defnc IntegerField
  [{:keys [render/field onChange]
    :or {field WrappedField
         onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ field
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ NumberInput
          {:ref input
           & (->
              props
              (dissoc :name :className :style)
              (assoc :onChange
                     (fn [e]
                       (some->>
                        (.. e -target -value)
                        not-empty
                        (re-find #"-?\d+[\.|,]*\d*")
                        (js/parseFloat)
                        onChange))))}))))


(defstyled integer-field IntegerField {:input {:border "none"}} --themed)

(defnc FloatField
  [{:keys [render/field onChange]
    :or {field WrappedField
         onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ field
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ NumberInput
          {:ref input
           & (->
              props
              (dissoc :name :className :style)
              (assoc :onChange
                     (fn [e]
                       (some->>
                        (.. e -target -value)
                        not-empty
                        (re-find #"-?\d+[\.|,]*\d*")
                        (js/parseFloat)
                        onChange))))}))))


(defstyled float-field FloatField {:input {:border "none"}} --themed)


(defstyled dropdown-field-wrapper field-wrapper
  {:position "relative"
   :padding "4px 16px 4px 4px"
   :cursor "pointer"}
  --themed)


(defnc DropdownFieldInput
  [props]
  ($ DropdownInput
     {:render/decorator DropdownElementDecorator
      :render/wrapper dropdown-field-wrapper
      & props}
     ($ dropdown-element-decorator {:className "decorator"})))

(defnc DropdownField
  [{:keys [render/field render/input]
    :or {field default-field
         input DropdownFieldInput}
    :as props}]
  ($ field {& props}
     ($ DropdownElement
        {:className "dropdown"
         :render/input input
         & (dissoc props :name :className :style)})))


(defstyled multiselect-wrapper field-wrapper
  {:position "relative"
   :padding "4px 16px 4px 4px"
   :border "1px solid black"
   :min-width 100
   ".tags" {:display "flex"
            :flex-direction "row"
            :flex-wrap "wrap"
            :align-items "baseline"
            (str autosize-input) {:align-self "center"}}}
  --themed)


(defnc MultiselectElement
  [{rinput :render/input
    rpopup :render/popup
    roption :render/option
    :or {rinput DropdownInput
         rpopup DropdownPopup
         roption Tag
         search-fn str}
    :keys [className context-fn search-fn disabled]
    :as props}]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        {:keys [open!
                remove!
                options
                area]
         :as multiselect} (multiselect/use-multiselect
                           (assoc props :search-fn search-fn))]
    (provider
     {:context *dropdown*
      :value multiselect}
     (provider
      {:context *dropdown-area*
       :value [area-position set-area-position!]}
      (d/div
       {:className className}
       (map
        (fn [option]
          ($ roption
             {:key (search-fn option)
              :value option
              :onRemove #(remove! option)
              :context (if disabled :stale
                           (when (fn? context-fn)
                             (context-fn option)))}))
        (:value props))
       ($ popup/Area
          {:ref area
           :onClick #(when-not (empty? options) (open!))
           :className "dropdown"}
          (when (not-empty options) ($ rinput))
          ($ rpopup)))))))


(defnc MultiselectField
  [{:keys [render/field]
    :or {field WrappedField} :as props}]
  ($ field {& props}
     ($ MultiselectElement
        {:render/wrapper multiselect-wrapper
         :render/popup DropdownPopup
         :render/option Tag
         :className "multiselect"
         & (dissoc props :name :className :style)})))


(defstyled multiselect-field MultiselectField
  {".multiselect"
   {:display "flex"
    :align-items "center"
    :flex-wrap "wrap"
    :min-height 30}}
  --themed)


(defstyled text-area-wrapper field-wrapper
  {:flex-grow "1"
   :textarea
   {:overflow "hidden"
    :border "none"
    :resize "none"
    :font-size "12"}})


(defnc TextareaField
  [{:keys [render/field
           render/wrapper
           style]
    :or {field default-field
         wrapper text-area-wrapper}
    :as props} _ref]
  {:wrap [(react/forwardRef)]}
  ($ field {& props}
     ($ wrapper
        ($ TextAreaElement
           {:spellCheck false
            :auto-complete "off"
            :style style
            :className "input"
            & (cond->
               (->
                props
                (dissoc :name :style :className)
                (update :value #(or % "")))
                _ref (assoc :ref _ref))}))))


(defstyled textarea-field TextareaField
  {".input" {:width "100%"}})

;; TIMESTAMPS

(defnc TimestampInput
  [{:keys [value
           placeholder
           className
           opened
           format]
    :or {format :datetime-short}}]
  (let [{:keys [open]} (hooks/use-context *calendar-control*)
        disabled (hooks/use-context *calendar-disabled*)
        translate (use-translate)]
    (d/div
     {:onClick open
      :className (str className (when opened (str " opened")))}
     ($ autosize-input
        {:className "input"
         :readOnly true
         :value (when (some? value) (translate format value))
         :spellCheck false
         :auto-complete "off"
         :disabled disabled
         :placeholder placeholder}))))

(defnc TimestampTime
  [{:keys [className hour minute]}]
  (let [hour (or hour 0)
        minute (or minute 0)
        {:keys [on-time-change]} (use-calendar-events)
        disabled (hooks/use-context *calendar-disabled*)]
    (d/div
     {:className className}
     ($ mask-input
        {:value (gstr/format "%02d:%02d" hour minute)
         :disabled disabled
         :mask (gstr/format "%02d:%02d" 0 0)
         :delimiters #{\:}
         :constraints [#"([0-1][0-9])|(2[0-3])" #"[0-5][0-9]"]
         :onChange (fn [time-]
                     (let [[h m] (map js/parseInt (clojure.string/split time- #":"))]
                       (when (ifn? on-time-change)
                         (on-time-change {:hour h :minute m}))))}))))

(defstyled timestamp-time TimestampTime
  {:display "flex"
   :justify-content "center"
   :align-items "center"
   :input {:max-width 40}
   :font-size "12"
   :margin "3px 0 5px 0"
   :justify-self "center"})


(defnc TimestampClear
  [{:keys [className]}]
  (let [{:keys [on-clear]} (use-calendar-events)
        disabled (hooks/use-context *calendar-disabled*)]
    (d/div
     {:className className}
     ($ fa {:icon faTimes
            :onClick (when-not disabled on-clear)}))))


(defstyled timestamp-clear TimestampClear
  {:color "white"
   :width 15
   :height 15
   :cursor "pointer"
   :padding 4
   :display "flex"
   :justify-self "flex-end"
   :justify-content "center"
   :align-items "center"}
  --themed)

(defnc TimestampCalendar
  [{:keys [year month day-in-month className]}]
  (let [now (-> (vura/date) vura/time->value)
        year (or year (vura/year? now))
        month (or month (vura/month? now))
        day-in-month (or day-in-month (vura/day-in-month? now))
        days (hooks/use-memo
              [year month]
              (vura/calendar-frame
               (vura/date->value (vura/date year month))
               :month))
        {:keys [on-next-month on-prev-month]} (use-calendar-events)]
    (d/div
     {:className className}
     (d/div
      {:className "header-wrapper"}
      (d/div
       {:className "header"}
       (d/div
        {:className "years"}
        ($ fa
           {:icon faAngleLeft
            :pull "left"
            :className "button"
            :onClick on-prev-month})
        ($ calendar-year-dropdown {:value year}))
       (d/div
        {:className "months"}
        ($ calendar-month-dropdown {:value month})
        ($ fa
           {:icon faAngleRight :pull "right"
            :className "button"
            :onClick on-next-month}))))
     (d/div
      {:className "content-wrapper"}
      (d/div
       {:className "content"}
       ($ calendar-month {:value day-in-month :days days}))))))

(defstyled timestamp-calendar TimestampCalendar
  {:display "flex"
   :flex-direction "column"
   :border-radius 3
   :padding 7
   :width 230
   :height 190
   (str popup/dropdown-container) {:overflow "hidden"}
   ".header-wrapper" {:display "flex" :justify-content "center" :flex-grow "1"}
   ".header"
   {:display "flex"
    :justify-content "space-between"
    :width 200
    :height 38
    ".years"
    {:position "relative"
     :display "flex"
     :align-items "center"}
    ".months"
    {:position "relative"
     :display "flex"
     :align-items "center"}}
   ".content-wrapper"
   {:display "flex"
    :height 150
    :justify-content "center"
    :flex-grow "1"}})

(defnc TimestampPopup
  [{:keys [year month day-in-month hour minute className]
    rcalendar :render/calendar
    rtime :render/time
    rclear :render/clear
    wrapper :render/wrapper
    :or {rcalendar timestamp-calendar
         rtime timestamp-time
         rclear timestamp-clear
         wrapper dropdown-popup}}
   popup]
  {:wrap [(react/forwardRef)]}
  ($ popup/Element
     {:ref popup
      :className (str className " animated fadeIn faster")
      :wrapper wrapper
      :preference popup/cross-preference}
     ($ rcalendar
        {:year year
         :month month
         :day-in-month day-in-month})
     (when (or rtime rclear)
       (d/div
        {:style
         {:display "flex"
          :flex-grow "1"
          :justify-content "center"}}
        (when rtime
          ($ rtime
             {:hour hour
              :minute minute}))
        (when rclear ($ rclear))))))


(defhook use-timestamp-events
  [set-timestamp! {:keys [day-in-month year selected] :as timestamp}]
  (hooks/use-memo
   [timestamp]
   (let [timestamp (if (nil? timestamp)
                     (-> (vura/date) vura/time->value vura/midnight vura/day-time-context)
                     timestamp)]
     {:on-clear #(set-timestamp! nil)
      :on-next-month
      (fn []
        (let [{:keys [day-in-month days-in-month]} timestamp
              value (vura/context->value (assoc timestamp :day-in-month 1))
              value' (+ value (vura/days days-in-month))
              {:keys [days-in-month] :as timestamp'} (vura/day-time-context value')]
          (set-timestamp! (assoc timestamp'
                                 :day-in-month (min day-in-month days-in-month)
                                 :selected selected))))
       ;;
      :on-prev-month
      (fn []
        (let [{:keys [day-in-month]} timestamp
              value (vura/context->value (assoc timestamp :day-in-month 1))
              value' (- value (vura/days 1))
              {:keys [days-in-month] :as timestamp'} (vura/day-time-context value')]
          (set-timestamp! (assoc timestamp'
                                 :day-in-month (min day-in-month days-in-month)
                                 :selected selected))))
       ;;
      :on-day-change #(set-timestamp!
                       (assoc
                        timestamp
                        :selected (-> timestamp
                                      (assoc :day-in-month %)
                                      vura/context->value
                                      vura/value->time)
                        :day-in-month %))
      :on-year-change #(set-timestamp! (assoc timestamp :year %))
      :on-month-change (fn [month]
                         (let [v (vura/utc-date-value year month)
                               {last-day :days-in-month} (vura/day-context v)]
                           (set-timestamp!
                            (assoc timestamp
                                   :month month
                                   :day-in-month (min day-in-month last-day)))))
      :on-time-change #(set-timestamp! (merge timestamp %))})))


(comment
  (def n (vura/date)))

(defnc TimestampElement
  [{:keys [value
           onChange
           disabled
           read-only]
    rfield :render/field
    rpopup :render/popup
    :or {rfield TimestampInput
         rpopup TimestampPopup}
    :as props}]
  (let [[{:keys [year month day-in-month]
          :as state} set-state!]
        (hooks/use-state
         (some->
          value
          vura/time->value
          vura/day-time-context))
        ;;
        [opened set-opened!] (hooks/use-state false)
        popup (hooks/use-ref nil)
        ;;
        area (hooks/use-ref nil)
        ;;
        set-state! (hooks/use-memo
                    [disabled read-only]
                    (if (or disabled read-only)
                      (fn [& _])
                      set-state!))
        selected? (hooks/use-memo
                   [year day-in-month month]
                   (fn [props]
                     (=
                      (select-keys
                       props
                       [:day-in-month :year :month])
                      {:year year
                       :day-in-month day-in-month
                       :month month})))
        events (use-timestamp-events set-state! state)
        cache (hooks/use-ref state)]
    ;; TODO - timestamp should be a little more complex than this
    ;; It shouldn't have single popup props since those props are used
    ;; to navigate calendar. Calendar itself should have props that are
    ;; responsible for calendar navigation, but that affects use-calendar-events
    ;; hook. Point is that two states are required. One for calendar and one for
    ;; current value context (currently implemented)
    ;; Good enough for now lets move on!
    (hooks/use-effect
     [value]
     (if (some? value)
       (let [ov (-> state
                    vura/context->value
                    vura/value->time)]
         (when-not (= value ov)
           (-> value
               vura/time->value
               vura/day-time-context
               set-state!)))
       (set-state! nil)))
    ;;
    (popup/use-outside-action
     opened area popup
     (fn [e]
        ;; FIXME  - Don't know how to do this more elegant
        ;; This will prevent updating when another dropdown
        ;; is event target
       (when (.contains js/document.body (.-target e))
         (set-opened! false)
         (when (and (ifn? onChange) (not= @cache value))
           (onChange @cache)))))
    ;;
    (hooks/use-effect
     [state]
     (if state
       (reset! cache (-> state vura/context->value vura/value->date))
       (reset! cache nil)))
    ;;
    (provider
     {:context *calendar-control*
      :value {:open #(set-opened! true)
              :close #(do
                        (set-opened! false)
                        (when (and (ifn? onChange) (not= @cache value))
                          (onChange @cache)))}}
     (provider
      {:context *calendar-selected*
       :value selected?}
      (provider
       {:context *calendar-events*
        :value events}
       (provider
        {:context *calendar-disabled*
         :value disabled}
        (provider
         {:context *calendar-opened*
          :value opened}
         ($ popup/Area
            {:ref area}
            ($ rfield {:opened opened & props})
            (when (and (not read-only) (not disabled) opened)
              ($ rpopup {:ref popup & state}))))))))))


(defnc TimestampFieldInput
  [{:keys [placeholder value format]
    :or {format :datetime-full}
    :as props}]
  (let [{:keys [open]} (hooks/use-context *calendar-control*)
        disabled (hooks/use-context *calendar-disabled*)
        translate (use-translate)
        input (hooks/use-ref nil)]
    ($ dropdown-field-wrapper
       {:onClick (fn []
                   (when @input (.focus @input))
                   (open))
        :className (str (:className props)
                        (when (:opened props) " opened")
                        (when disabled " disabled"))}
       ($ autosize-input
          {:ref input
           :className "input"
           :readOnly true
           :value (when (some? value) (translate format value))
           :spellCheck false
           :auto-complete "off"
           :disabled disabled
           :placeholder placeholder}))))

(defnc TimestampField
  [{:keys [value placeholder disabled
           read-only onChange format
           render/field]
    :or {field default-field
         format :datetime-full}
    :as props}]
  ($ field
     {& props}
     ($ TimestampElement
        {:value value
         :onChange onChange
         :placeholder placeholder
         :disabled disabled
         :read-only read-only
         :format format
         :className "data"
         :render/field TimestampFieldInput})))

;;

(defstyled period-container "div"
  {:display "flex"
   :flex-direction "row"})

;;

(defstyled time-period-popup "div"
  {:display "flex"
   :flex-direction "row"
   :border-radius 3}
  --themed)



(defnc PeriodElement
  [{:keys [className]
    rcalendar :render/calendar
    rtime :render/time
    rclear :render/clear
    :or {rcalendar timestamp-calendar
         rtime timestamp-time
         rclear timestamp-clear}}]
  {:wrap [(react/forwardRef)]}
  (let [{start-events :start
         end-events :end} (use-calendar-events)
        [start end] (use-calendar-state)]
    (d/div
     {:className className}
     (provider
      {:context *calendar-events*
       :value start-events}
      (d/div
       {:className "start"}
       ($ rcalendar
          {:year (:year start)
           :month (:month start)
           :day-in-month (:day-in-month start)})
       (when (or rtime rclear)
         (d/div
          {:style
           {:display "flex"
            :flex-grow "1"
            :justify-content "center"}}
          (when rtime
            ($ rtime
               {:hour (:hour start)
                :minute (:minute start)}))
          (when (and (some? start) rclear) ($ rclear))))))
     (provider
      {:context *calendar-events*
       :value end-events}
      (d/div
       {:className "end"}
       ($ rcalendar
          {:year (:year end)
           :month (:month end)
           :day-in-month (:day-in-month end)})
       (when (or rtime rclear)
         (d/div
          {:style
           {:display "flex"
            :flex-grow "1"
            :justify-content "center"}}
          (when rtime
            ($ rtime
               {:hour (:hour end)
                :minute (:minute end)}))
          (when (and (some? end) rclear) ($ rclear)))))))))


(defnc PeriodPopup
  [{:keys [render/wrapper className]
    :or {wrapper dropdown-popup}
    :as props}
   popup]
  {:wrap [(react/forwardRef)]}
  ($ popup/Element
     {:ref popup
      :className (str className " animated fadeIn faster")
      :wrapper wrapper
      :preference popup/cross-preference}
     ($ PeriodElement {:className "period" & props})))

(defstyled period-popup PeriodPopup
  {".period"
   {:display "flex"
    :flex-direction "row"}})


(defnc PeriodInput
  [{:keys [disabled
           placeholder
           className
           opened
           format]
    [from to :as value] :value
    :or {format :datetime-short}}]
  (let [{:keys [open]} (use-calendar-events)
        translate (use-translate)
        input (hooks/use-ref nil)]
    (d/div
     {:onClick (fn []
                 (when @input (.focus @input))
                 (open))
      :className (str className (when opened (str " opened")))}
     ($ autosize-input
        {:ref input
         :className "input"
         :readOnly true
         :value (if (or (nil? value) (every? nil? value))
                  ""
                  (str
                   (if from (translate format from) " ")
                   "-"
                   (if to (translate format to) " ")))
         :spellCheck false
         :auto-complete "off"
         :disabled disabled
         :placeholder placeholder}))))

;;

(defnc PeriodElementProvider
  [{:keys [disabled
           read-only
           onChange]
    [upstream-start upstream-end] :value
    :or {upstream-start nil upstream-end nil}
    :as props}]
  (let [[[{start-value :selected :as start} {end-value :selected :as end}] set-state!]
        (hooks/use-state [{:selected upstream-start} {:selected upstream-end}])
        ;;
        set-state! (hooks/use-memo
                    [disabled read-only]
                    (if (or disabled read-only)
                      (fn [& _])
                      set-state!))
        selected? (hooks/use-memo
                   [start-value end-value]
                   (fn [data]
                     (letfn [(->value [{:keys [year month day-in-month]}]
                               (vura/utc-date-value year month day-in-month))]
                       (let [start (when start-value (vura/date->value start-value))
                             current (->value data)
                             end (when end-value (vura/date->value end-value))]
                         (cond
                           (nil? start) (<= current end)
                           (nil? end) (>= current start)
                           :else
                           (or
                            (= start current)
                            (= end current)
                            (<= start current end)))))))
        [set-start! set-end!] (hooks/use-memo
                               :once
                               [(fn [value]
                                  (set-state! assoc 0 value))
                                (fn [value]
                                  (set-state! assoc 1 value))])
        start-events (use-timestamp-events set-start! start)
        end-events (use-timestamp-events set-end! end)]
    (hooks/use-effect
     [start-value end-value]
     (onChange [start-value end-value]))
    (hooks/use-effect
     [upstream-start upstream-end]
     (when (= [nil nil] [upstream-start upstream-end])
       (set-state! [nil nil])))
    ;;
    (provider
     {:context *calendar-selected*
      :value selected?}
     (provider
      {:context *calendar-events*
       :value {:start start-events
               :end end-events}}
      (provider
       {:context *calendar-state*
        :value [start end]}
       (c/children props))))))


(defnc PeriodDropdown
  [{:keys [disabled
           value
           read-only
           onChange]
    rfield :render/field
    rpopup :render/popup
    :or {rfield PeriodInput
         rpopup period-popup}
    :as props}]
  (let [[state] (use-calendar-state)
        area (hooks/use-ref nil)
        [opened set-opened!] (hooks/use-state false)
        popup (hooks/use-ref nil)
        cache (hooks/use-ref value)]
    ;;
    (hooks/use-effect
     [state]
     (if state
       (reset! cache (-> state vura/context->value vura/value->date))
       (reset! cache nil)))
    ;;
    (popup/use-outside-action
     opened area popup
     #(do
        (set-opened! false)
        (when (and
               (ifn? onChange)
               (not= @cache value))
          (onChange @cache))))
    ($ popup/Area
       {:ref area}
       (when rfield
         ($ rfield {:opened opened  & props}))
       (when (or
              (nil? rfield)
              (and (not read-only) (not disabled) opened))
         ($ rpopup {:ref popup :value state})))))


(defnc PeriodDropdownElement
  [props]
  ($ PeriodElementProvider
     {& props}
     ($ PeriodDropdown {& props})))

;; Avatar

; (defnc Avatar
;   [{:keys [avatar className]}]
;   (let [avatar' (use-avatar avatar)] 
;     (d/img 
;       {:class className
;        :src avatar'})))


(def ^:dynamic *avatar-root* (create-context ""))


(defhook use-avatar-root
  []
  (hooks/use-context *avatar-root*))

(defnc Avatar
  [{:keys [avatar className]}]
  (let [root (use-avatar-root)
        avatar' (when avatar
                  (if (re-find #"^data:image" avatar)
                    avatar
                    (str root avatar)))]
    (d/img
     {:class className
      :src avatar'})))

(defstyled avatar Avatar
  nil
  (fn [{:keys [theme size]
        :or {size 36}}]
    (let [size' (case size
                  :small 20
                  :medium 36
                  :large 144
                  size)]
      (case (:name theme)
        {:border-radius 20
         :width size'
         ; :margin "0 5px"
         :height size'}))))

;; User field
(defnc UserElement
  [props]
  ($ DropdownElement
     {:search-fn :name
      :render/img avatar
      & (dissoc props :search-fn)}))


(defstyled user UserElement
  {:display "flex"
   :align-items "center"}
  --themed)


(defnc UserField
  [{:keys [render/field]
    :or {field WrappedField}
    :as props}]
  ($ field
     {& props}
     ($ UserElement
        {:className "input"
         & (->
            props
            (dissoc :name :style :className)
            (update :value #(or % "")))})))

;; Group field


(defnc GroupElement
  [props]
  (let [search-fn :name]
    ($ DropdownElement
       {:search-fn search-fn
        & (dissoc props :search-fn)})))

(defstyled group GroupElement
  {:display "flex"
   :align-items "center"}
  --themed)


(defnc GroupField
  [{:keys [render/field]
    :or {field WrappedField}
    :as props}]
  ($ field
     {& props}
     ($ GroupElement
        {:className "input"
         & (->
            props
            (dissoc :name :style :className)
            (update :value #(or % "")))})))

;; User multiselect


(defnc UserMultiselectElement
  [props]
  (let [search-fn :name
        display-fn (fn [option] ($ avatar {& option}))]
    ($ MultiselectElement
       {:search-fn search-fn
        :display-fn display-fn
        & (dissoc props :search-fn :display-fn)})))

(defstyled user-multiselect UserMultiselectElement
  {:display "flex"
   :align-items "center"}
  --themed)


(defnc UserTagContent
  [{:keys [className value]}]
  (d/div
   {:className className}
   ($ avatar
      {:size :small
       & value})
   (:name value)))

(defstyled user-tag-content UserTagContent
  {:display "flex"
   :align-items "center"})

(defnc UserTag
  [props]
  ($ Tag {:render/content user-tag-content & props}))

(defstyled user-tag UserTag
  $tag
  --themed)


(defnc UserDropdownOption
  [{:keys [option] :as props} ref]
  {:wrap [(react/forwardRef)]}
  ($ dropdown-option
     {:ref ref
      & (dissoc props :ref :option)}
     ($ avatar {:size :small & option})
     (:name option)))


(defstyled user-dropdown-option UserDropdownOption
  {(str avatar) {:margin-right 5}}
  --themed)


(defnc UserDropdownAvatar
  [{:keys [value]}]
  ($ avatar {:size :small & value}))


(defnc UserDropdownInput
  [props]
  ($ DropdownInput
     {:render/img UserDropdownAvatar
      & props}))

(defstyled user-dropdown-input UserDropdownInput
  {:font-size "12"
   (str avatar) {:margin-right 5}}
  --themed)


(defnc UserDropdownPopup
  [props]
  ($ DropdownPopup
     {:render/option user-dropdown-option
      & props}))


(defstyled user-dropdown-popup UserDropdownPopup
  {:max-height 250})

(defnc UserMultiselectField
  [{:keys [render/field]
    render-option :render/option
    render-input :render/input
    render-popup :render/popup
    :or {field WrappedField
         render-option user-tag
         render-input user-dropdown-input
         render-popup user-dropdown-popup}
    :as props}]
  ($ field {& props}
     (let [search-fn :name
           display-fn (fn [option] ($ avatar {& option}))]
       ($ MultiselectElement
          {:search-fn search-fn
           :display-fn display-fn
           :render/option render-option
           :render/input render-input
           :render/popup render-popup
           :className "multiselect"
           & (dissoc props :name :className :style)}))))


(defstyled user-multiselect-field UserMultiselectField
  {".multiselect"
   {:display "flex"
    :align-items "center"
    :flex-wrap "wrap"
    :min-height 30}}
  --themed)


;; Group multiselect
(defnc GroupMultiselectElement
  [props]
  (let [search-fn :name]
    ($ MultiselectElement
       {:search-fn search-fn
        & (dissoc props :search-fn)})))

(defstyled group-multiselect GroupMultiselectElement
  {:display "flex"
   :align-items "center"}
  --themed)



(defnc Search
  [{:keys [value icon on-change idle-timeout className onChange]
    :or {idle-timeout 500
         value ""
         icon faSearch
         onChange identity}
    :as props}]
  (let [on-change (or on-change onChange identity)
        [input set-input!] (use-idle "" #(on-change %) idle-timeout)]
    (hooks/use-effect
     [value]
     (when (not= value input)
       (set-input! value)))
    (d/div
     {:className className}
     (d/div
      {:class "value"}
      ($ AutosizeInput
         {& (merge
             (dissoc props :className)
             {:value input
              :on-change (fn [e] (set-input! (.. e -target -value)))})}))
     (d/div
      {:class "icon"}
      ($ FontAwesomeIcon {:icon icon})))))


(defstyled search Search
  {:display "flex"
   :align-items "center"
   :padding "3px 5px"
   :border "1px solid"
   :border-radius 5
   ".value" {:order 2
             :input {:min-width 250}}
   ".icon" {:order 1 :margin-right "5px"}
   :input {:outline "none" :border "none"}}
  --themed)

;; TABLE



(defn- same-cell-data? [a b]
  (= (:cell/data a) (:cell/data b)))


(defn- same-cell-onChange? [a b]
  (= (:onChange a) (:onChange b)))

(defn- same-cell-style? [a b]
  (=
   (:style (:cell/column a))
   (:style (:cell/column b))))

(defn- same-cell? [a b]
  (and
   (same-cell-data? a b)
   (same-cell-style? a b)
   (same-cell-onChange? a b)))





(defnc DropdownCell
  [{:keys [cell/data cell/column
           cell/disabled cell/read-only]}]
  {:wrap [(memo same-cell?)]}
  ($ DropdownElement
     {:value data
      :disabled disabled
      :read-only read-only
      & (dissoc column :disabled :read-only)}))

(defstyled dropdown-cell DropdownCell
  nil)


(defnc TextCell
  [{:keys [cell/data onChange cell/column className
           cell/disabled cell/read-only]
    :or {onChange identity}}]
  {:wrap [(memo same-cell?)]}
  (let [{:keys [placeholder style]
         :or {placeholder "Value..."}} column]
    ($ TextAreaElement
       {:placeholder placeholder
        :className className
        :readOnly read-only
        :disabled disabled
        :spellCheck false
        :auto-complete "off"
        :style (->js style)
        :value (or data "")
        :onChange (fn [e]
                    (when (ifn? onChange)
                      (onChange (.. e -target -value))))})))


(defstyled text-cell TextCell
  {:line-height 12
   :resize "none"
   :border "none"})


(defnc IntegerCell
  [{:keys [cell/data onChange cell/column
           cell/disabled cell/read-only className]
    :or {onChange identity}}]
  {:wrap [(memo same-cell?)]}
  (let [{:keys [placeholder]
         :or {placeholder "Value..."}} column
        [cached update!] (use-idle
                          (str data)
                          #(try
                             (if (or (= :NULL %) (empty? %))
                               (onChange :NULL)
                               (onChange (js/parseInt %)))
                             (catch js/Error _
                               (.error js/console (str "Couldn't parse number " %)))))]
    (hooks/use-effect
     [data]
     (let [i (str data)]
       (when-not (= cached i)
         (update! i))))
    ($ NumberInput
       {:className className
        :value cached
        :placeholder placeholder
        :disabled disabled
        :read-only read-only
        :onChange (fn [e] (update! (re-find #"[0-9]+" (.. e -target -value))))})))

;;

(defstyled integer-cell IntegerCell nil)

;;

(defnc FloatCell
  [{:keys [cell/data onChange cell/column
           cell/disabled cell/read-only className]
    :or {onChange identity}}]
  {:wrap [(memo same-cell?)]}
  (let [{:keys [placeholder]
         :or {placeholder "Value..."}} column
        [cached update!] (use-idle
                          (str data)
                          #(try
                             (if (or (= :NULL %) (empty? %))
                               (onChange :NULL)
                               (onChange (js/parseFloat %)))
                             (catch js/Error _
                               (.error js/console (str "Couldn't parse number " %)))))]
    (hooks/use-effect
     [data]
     (let [i (str data)]
       (when-not (= cached i)
         (update! i))))
    ($ NumberInput
       {:className className
        :value cached
        :placeholder placeholder
        :disabled disabled
        :read-only read-only
        :onChange (fn [e]
                    (update! (re-find #"[0-9]*[\.]{0,1}[0-9]*" (.. e -target -value))))})))

;;

(defstyled float-cell FloatCell
  nil)

;;

(defnc BooleanCell
  [{:keys [cell/data onChange cell/disabled
           cell/read-only className]}]
  {:wrap [(memo same-cell?)]}
  ($ checkbox
     {:active data
      :disabled disabled
      :read-only read-only
      :className className
      :onClick #(onChange (not data))}))

(defstyled boolean-cell BooleanCell
  nil)


;;

(defnc TimestampCell
  [{:keys [cell/data cell/column onChange
           cell/disabled cell/read-only cell/format className]
    :or {onChange identity
         format :datetime-full}}]
  (let [{:keys [placeholder]
         :or {placeholder "Date..."}} column]
    ($ TimestampElement
       {:value data
        :onChange onChange
        :format format
        :read-only read-only
        :placeholder placeholder
        :className className
        :disabled disabled})))

;;

(defstyled timestamp-cell TimestampCell
  {:display "flex"
   :align-items "center"
   ; :flex-grow "1"
   ".decorator" {:margin-left "auto"}
   :input {:padding 0}}
  --themed)

;;

(defn --themed-cell
  [{:keys [theme $selected?]}]
  (case (:name theme)
    (cond->
     {":hover" {:background-color "#f7f7f7"}}
      $selected? (assoc :background-color "#f7f7f7"))))


;;

(defnc UserCell
  [{:keys [cell/data className cell/read-only cell/disabled] :as props}]
  {:wrap [(memo same-cell-data?)]}
  (if data
    ($ UserElement
       {:value (assoc data :size :small)
        :read-only read-only
        :disabled disabled
        :className className
        & (dissoc props :className)})
    (d/div "-")))

(defstyled user-cell UserCell
  {:font-size "12"
   :img {:height 20 :width 20}}
  --themed)


(defnc Pagination
  [{:keys [data className set-pagination!]
    {:keys [page page-size
            next? previous?
            page-count total-count]
     :as pagination} :pagination}]
  (when pagination
    (d/div
     {:class className}
     (d/button
      {:onClick #(set-pagination! {:page 0})
       :className "start"
       :disabled (not previous?)}
      ($ fa {:icon faAngleDoubleLeft}))
     (d/button
      {:onClick #(set-pagination! {:page (dec page)})
       :className "previous"
       :disabled (not previous?)}
      ($ fa {:icon faAngleLeft}))
     (d/button
      {:onClick #(set-pagination! {:page (inc page)})
       :className "next"
       :disabled (not next?)}
      ($ fa {:icon faAngleRight}))
     (d/button
      {:onClick #(set-pagination! {:page (dec page-count)})
       :className "end"
       :disabled (not next?)}
      ($ fa {:icon faAngleDoubleRight}))
     (d/span
      (goog.string/format
       "Showing %d - %d of %d results"
       (* page page-size)
       (+ (* page page-size) (count data))
       total-count))
     (d/select
      {:value page-size
       :className "view-size"
       :onChange (fn [e] (set-pagination! {:page-size (js/Number (.. e -target -value))}))}
      (map
       (fn [option]
         (d/option
          {:key option
           :value option}
          (str "Show " option)))
       (range 10 60 10)))
     (d/span (str "| Page: "))
     ($ IdleInput
        {:placeholder "?"
         :className "pag"
         :spell-check false
         :auto-complete "off"
         :type "number"
         :value (inc page)
         :on-change (fn [page]
                      (let [page (if page page 0)
                            np (min (max 0 (dec page)) page-count)]
                        (set-pagination! {:page np})))}))))



(defhook use-cell-render
  [{:keys [render type]}]
  (if (some? render) render
      (case type
        "boolean" boolean-cell
      ; "currency" currency-cell
      ; "enum" enum-cell
        "float" float-cell
      ; "hashed" HashedCell
        "int" integer-cell
      ; "uuid" uuid-cell
        "string" text-cell
        "timestamp" timestamp-cell)))

(defstyled table "div"
  {:padding "0 14px"
   :input {:border "none" :outline "none"}
   :font-size "12"})


(defstyled table-row table/FRow
  {:border-bottom "1px solid #f1f1f1"
   :transition "border .3s ease-in"})

(defstyled table-header "div"
  {:padding "0 14px"
   :font-weight "600"
   :margin-bottom 5
   (str table-row)
   {:padding-bottom 3
    :border-color "transparent"
    :border-bottom "1px solid"
    :margin-bottom 5}}
  --themed)

(defstyled table-header-cell table/FHeader
  {:text-transform "uppercase"}
  --themed)

(defstyled table-body "div"
  {:padding "0 14px"})

(defstyled table-cell table/FCell
  {:padding "2px 0"}
  --themed-cell)



(defnc CardAction
  [{:keys [className onClick tooltip disabled]
    _icon :icon
    render-tooltip :render/tooltip
    :or {render-tooltip action-tooltip}}]
  ($ render-tooltip
     {:message tooltip
      :disabled (or (empty? tooltip) disabled)}
     (d/div
      {:className className}
      (d/div
       {:className "action"
        :onClick onClick}
       ($ fa {:icon _icon})))))


(let [size 32
      inner 26
      icon 14]
  (defstyled card-action CardAction
    {:height size
     :width size
     :display "flex"
     :justify-content "center"
     :align-items "center"
     :border-radius size
     ".action"
     {:width inner
      :height inner
      :border-radius inner
      :cursor "pointer"
      :transition "color,background-color .2s ease-in-out"
      :display "flex"
      :justify-content "center"
      :align-items "center"
      :svg {:height icon
            :width icon}}}
    --themed))


(defnc CardActions
  [{:keys [className] :as props}]
  (d/div
   {:className className}
   (d/div
    {:className "wrapper"}
    (c/children props))))


(defstyled card-actions CardActions
  {:position "absolute"
   :top -16
   :right -16
   ".wrapper"
   {:display "flex"
    :flex-direction "row"
    :justify-content "flex-end"
    :align-items "center"}})


(defstyled card "div"
  {:position "relative"
   :display "flex"
   :flex-direction "column"
   :max-width 300
   :min-width 180
   :padding "10px 10px 5px 10px"
   :background-color "#eaeaea"
   :border-radius 5
   :transition "box-shadow .2s ease-in-out"
   ":hover" {:box-shadow "1px 4px 11px 1px #ababab"}
   (str card-actions) {:opacity "0"
                       :transition "opacity .4s ease-in-out"}
   (str ":hover " card-actions) {:opacity "1"}
   ;;
   (str avatar)
   {:position "absolute"
    :left -10
    :top -10
    :transition "all .1s ease-in-out"}})

;;


;; Could be usefool

; (defn inject-handler [props evnts f]
;   (reduce
;     (fn [props' evt]
;       (if-let [oh (get props' evt)]
;         (if (fn? oh)
;           (assoc props' evt (fn [& args] (apply f args) (apply oh args)))
;           props')
;         props'))
;     props
;     evnts))

; (defn append-handler [props evnts f]
;   (reduce
;     (fn [props' evt]
;       (if-let [oh (get props' evt)]
;         (if (fn? oh)
;           (assoc props' evt (fn [& args] (apply oh args) (apply f args)))
;           props')
;         props'))
;     props
;     evnts))
