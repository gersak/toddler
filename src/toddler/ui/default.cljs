(ns toddler.ui.default
  (:require
    clojure.set
    clojure.string
    ; [clojure.data :refer [diff]]
    ; [clojure.core.async :as async]
    [goog.string :as gstr]
    [goog.string.format]
    [vura.core :as vura]
    [cljs-bean.core :refer [->clj ->js]]
    [helix.styled-components :refer [defstyled]]
    [helix.core
     :refer [$ defnc fnc provider
             defhook create-context memo]]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.hooks  :as hooks]
    [helix.spring :as spring]
    [toddler.app :as app]
    [toddler.hooks
     :refer [#_make-idle-service
             use-dimensions
             use-translate
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
    [toddler.elements.popup :as popup]
    [toddler.elements.tooltip :as tip]
    [toddler.elements.scroll :refer [SimpleBar]]
    [toddler.elements.date :as date]
    [toddler.ui.provider :as ui.provider]
    ["react" :as react]
    ["toddler-icons$default" :as icon]))

(def color
  (merge
    {:gray "#616161"
     :teal "#80d6d6"
     :blue "#85b7f1"
     :red "#fd6286"
     :orange "#ff872d"
     :yellow "#ffec72"
     :green "#0dda87"
     :asphalt "#9cb4d8"
     :white "white"
     :disabled "#bbbbbb"
     :link "rgba(44,29,191,0.78)"
     
     :color "#275f82"
     :background "white"}
    #:gray {:light "#f2eff2"
            :dark "#2c2c2c"}
    #:teal {:dark "#598ea7"
            :deep "#44666d"
            :saturated "#00cccc"}
    #:asphalt {:dark "#3562a2"
               :bleached "#598ea71a"}
    #:orange {:dark "#ff4007"}
    #:yellow {:dark "#FFDC00"
              :deep "#c7ac00"}
    #:white {:transparent "#ffffffee"}))

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
   :margin "5px 10px"
   ".field-name"
   {:color (color :gray)
    :user-select "none"
    :transition "all .3s ease-in-out"
    :font-size 12
    :font-weight "600"
    :text-transform "uppercase"}})


(defstyled field-wrapper
  "div"
  {:border "1px solid"
   :border-radius 2
   :margin-top 4
   :padding "4px 10px"
   :cursor "text"
   :input {:font-size "12"}
   :overflow "hidden"
   :border-color "#b3b3b3 !important"
   :background "#e5e5e5"
   :transition "all .3s ease-in-out"
   ":focus-within" {:border-color (str (color :teal) "!important")
                    :box-shadow (str "0 0 3px " (color :teal))
                    :background-color "transparent"}
   "input,textarea"
   {:color (color :gray)}})

(defstyled text-area-wrapper field-wrapper
  {:flex-grow "1"
   :textarea
   {:overflow "hidden"
    :border "none"
    :resize "none"
    :font-size "12"}})

(defnc TextareaField
  [{:keys [style]
    :as props} _ref]
  {:wrap [(react/forwardRef)]}
  ($ default-field {& props}
    ($ text-area-wrapper
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
  {:textarea {:font-family "Roboto"}})


(defnc WrappedField
  [{:keys [context]
    :as props}]
  ($ default-field {& props}
    ($ field-wrapper {:context context}
       (c/children props))))


(defstyled autosize-input AutosizeInput
  {:outline "none"
   :border "none"})


(defnc InputField
  [props]
  (let [_input (hooks/use-ref nil)]
    ($ WrappedField
       {:onClick (fn [] (when @_input (.focus @_input)))
        & props}
       ($ autosize-input
          {:ref _input
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           & (dissoc props :name :className :style)}))))


(defnc IntegerField
  [{:keys [onChange]
    :or {onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ WrappedField
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


(defstyled integer-field IntegerField {:input {:border "none"}})


(defnc FloatField
  [{:keys [onChange]
    :or {onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ WrappedField
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


(defstyled float-field FloatField {:input {:border "none"}})


(defstyled dropdown-field-wrapper field-wrapper
  {:position "relative"
   :padding "4px 16px 4px 4px"
   :cursor "pointer"})


(defstyled dropdown-element-decorator dropdown/Decorator
  {:position "absolute"
   :right 4 
   :top 7
   :transition "color .2s ease-in-out"
   :color (color :gray)
   "&.opened" {:color "transparent"}})


(defnc DropdownInput
  [props]
  ($ dropdown/Input
    {:render/wrapper dropdown-field-wrapper
     :render/input autosize-input
     & props}
    ($ dropdown-element-decorator {:className "decorator"})))


(defstyled dropdown-popup "div"
  {:display "flex"
   :flex-direction "column"
   :border-radius 3
   :padding 7
   :background-color "white"
   :box-shadow "0px 3px 10px -3px black"
   " .simplebar-scrollbar:before"
   {:background (color :gray)
    :pointer-events "none"}
   :max-height 400})


(defstyled dropdown-option
  "div"
  {:font-size "12"
   :display "flex"
   :justify-content "flex-start"
   :align-items "center"
   :color (color :gray) ;"#00b99a"
   :cursor "pointer"
   :background-color "white" 
   :transition "color .2s ease-in,background-color .2s ease-in"
   :padding "4px 6px 4px 4px"
   ; :border-radius 3
   ; :font-weight "500" 
   " :hover" {:color (color :gray) 
              :background-color "#d7f3f3"}
   ":last-child" {:border-bottom "none"}})

(defnc DropdownPopup
  [props]
  ($ dropdown/Popup
    {:render/popup dropdown-popup
     :render/option dropdown-option
     & props}
    (c/children props)))


(defnc DropdownField
  [props]
  ($ default-field {& props}
     ($ dropdown/Element
        {:className "dropdown"
         :render/input DropdownInput
         :render/popup DropdownPopup
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
            (str autosize-input) {:align-self "center"}}})


(defstyled multiselect-option multiselect/Option
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
   :border-radius 3
   :color "white"
   :background-color (color :teal)
   " .remove" {:color (color :teal/dark)
               :cursor "pointer"
               :transition "color .2s ease-in"
               :path {:cursor "pointer"}}
   " .remove:hover" {:color (color :red)}})

(defstyled dropdown-wrapper
  "div"
  {:display "flex"
   :justify-content "row"
   :align-items "center"})


(defnc MultiselectInput
  [props]
  ($ dropdown/Input
    {:render/input autosize-input
     :render/wrapper dropdown-wrapper 
     & props}
    (c/children props)))


(defnc MultiselectField
  [{:keys [render/field]
    :or {field WrappedField} :as props}]
  ($ field {& props}
    ($ multiselect/Element
       {:render/wrapper multiselect-wrapper
        :render/input MultiselectInput
        :render/popup DropdownPopup
        :render/option multiselect-option
        :className "multiselect"
        & (dissoc props :name :className :style)})))


(defstyled multiselect-field MultiselectField
  {".multiselect"
   {:display "flex"
    :align-items "center"
    :flex-wrap "wrap"
    :min-height 30}})


(defnc VanillaDropdownInput
  [props]
  ($ dropdown/Input
    {:render/input autosize-input
     & props}))


(defnc CalendarMonthDropdown
  [props]
  ($ date/CalendarMonthDropdown
    {& props
     :render/input VanillaDropdownInput
     :render/popup DropdownPopup}))


(defstyled calendar-month-dropdown CalendarMonthDropdown
  {:margin "5px 0"
   :cursor "pointer"
   :input {:cursor "pointer"
           :color (color :red)}})


(defnc CalendarYearDropdown
  [props]
  ($ date/CalendarYearDropdown
    {& props
     :render/input VanillaDropdownInput
     :render/popup DropdownPopup}))


(defstyled calendar-year-dropdown CalendarYearDropdown
  {:margin "5px 0"
   :cursor "pointer"
   :input {:cursor "pointer"
           :color (color :red)}})


(defstyled calendar-day date/CalendarDay
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
    ".empty" {:cursor "default"}
    "&.disabled, &:hover.disabled"
    {:background-color "white"
     :color (color :disabled)
     :border-color "transparent"
     :cursor "default"}
    ;;
    ":hover:not(.empty),&.selected"
    {:color "white"
     :background-color (color :teal/saturated)
     :border-collapse "collapse"
     :border (str "1px solid " (color :teal/dark))
     :border-radius 2
     :font-weight 500}
    "&.today"
    {:border "1px solid (color :teal)"}
    "&.weekend"
    {:color (color :red)}}
   :color (color :gray)
   :font-size 12})


(defnc CalendarWeek
  [props]
  ($ date/CalendarWeek
    {:render/day calendar-day
     & props}))


(defstyled calendar-week CalendarWeek
  {".week-days"
   {:display "flex"
    :flex-direction "row"}})


(defstyled calendar-month-header date/CalendarMonthHeader
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
     :border "1px solid transparent"}}
   ".day-wrapper .day"
   {:color (color :gray)
    :font-size 12
    "&.weekend"
    {:color (color :red)}}})


(defnc CalendarMonth
  [props]
  ($ date/CalendarMonth
    {:render/header calendar-month-header
     :render/week calendar-week
     & props}))


(defstyled calendar-month CalendarMonth
  {:display "flex"
   :flex-direction "column"
   :width 220
   ".week-days-header .day-wrapper .day"
   {:color (color :gray)}
   ".week-row .week-days .day-wrapper .day"
   {:color (color :gray)
    ;;
    "&.disabled, &:hover.disabled"
    {:background-color "white"
     :color (color :disabled)
     :border-color "transparent"
     :cursor "default"}
    ;;
    ":hover:not(.empty),&.selected"
    {:color "white"
     :background-color (color :teal/saturated)
     :border-collapse "collapse"
     :border (str "1px solid " (color :teal/dark))
     :border-radius 2
     :font-weight 500}}})



(defnc TimestampCalendar
  [props]
  ($ date/TimestampCalendar
    {:render/year-dropdown calendar-year-dropdown
     :render/month-dropdown calendar-month-dropdown
     :render/month calendar-month
     & props}))


(defstyled timestamp-calendar TimestampCalendar
  {:display "flex"
   :flex-direction "column"
   :border-radius 3
   :padding 7
   :width 230
   :height 190
   ; (str popup/dropdown-container) {:overflow "hidden"}
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


(defstyled timestamp-time date/TimestampTime
  {:display "flex"
   :justify-content "center"
   :align-items "center"
   :input {:max-width 40}
   :font-size "12"
   :margin "3px 0 5px 0"
   :justify-self "center"
   ".time" {:outline "none"
            :border "none"}})


(defstyled timestamp-clear date/TimestampClear
  {:width 15
   :height 15
   :padding 4
   :display "flex"
   :justify-self "flex-end"
   :justify-content "center"
   :align-items "center"
   :background-color (color :gray/light)
   :color "white"
   :transition "background .3s ease-in-out"
   :border-radius 20
   :cursor "pointer"
   ":hover" {:background-color (color :red)}})


(defnc TimestampPopup
  [props _ref]
  {:wrap [react/forwardRef]}
  ($ date/TimestampPopup
    {:ref _ref
     :render/calendar timestamp-calendar
     :render/time timestamp-time
     :render/clear timestamp-clear
     :render/wrapper dropdown-popup
     & props}))


(defnc TimestampFieldInput
  [props]
  ($ date/TimestampInput
    {:render/wrapper dropdown-field-wrapper
     & props}))


(defnc TimestampField
  [{:keys [value placeholder disabled
           read-only onChange format]
    :or {format :datetime-full}
    :as props}]
  ($ default-field
    {& props}
    ($ date/TimestampDropdown
       {:value value
        :onChange onChange
        :placeholder placeholder
        :disabled disabled
        :read-only read-only
        :format format
        :className "data"
        :render/popup TimestampPopup
        :render/field TimestampFieldInput})))


(defnc PeriodElement
  [props]
  ($ date/PeriodElement
    {& props
     :render/calendar timestamp-calendar
     :render/time timestamp-time
     :render/clear timestamp-clear}))


(defnc PeriodPopup
  [props _ref]
  {:wrap [react/forwardRef]}
  ($ date/PeriodPopup
    {& props
     :ref _ref
     :render/calendar timestamp-calendar
     :render/time timestamp-time
     :render/clear timestamp-clear
     :render/wrapper dropdown-popup}))


(defstyled period-popup PeriodPopup
  {".period"
   {:display "flex"
    :flex-direction "row"}})


(defnc PeriodFieldInput
  [props] 
  ($ date/PeriodInput
    {& props
     :render/wrapper dropdown-field-wrapper}))


(defnc PeriodField
  [{:keys [value placeholder disabled
           read-only onChange format]
    :or {format :medium-datetime}
    :as props}]
  ($ date/PeriodElementProvider
    {& props}
    ($ default-field
       {& props}
       ($ date/PeriodDropdown
          {:value value
           :onChange onChange
           :placeholder placeholder
           :disabled disabled
           :read-only read-only
           :format format
           :className "data"
           :render/popup PeriodPopup
           :render/field PeriodFieldInput}))))


(def components
  (merge
    #:field {:text textarea-field
             :input InputField
             :integer integer-field 
             :float float-field
             :dropdown DropdownField
             :multiselect multiselect-field
             :timestamp TimestampField
             :period PeriodField}))


(defnc Provider [props]
  ($ ui.provider/UI
    {:components components
     & props}))
