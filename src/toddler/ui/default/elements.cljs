(ns toddler.ui.default.elements
  (:require
    ["react" :as react]
    [helix.core
     :refer [$ defnc]]
    [helix.dom :as d]
    [helix.styled-components :refer [defstyled]]
    [helix.children :as c]
    [toddler.elements.input
     :refer [AutosizeInput]]
    [toddler.elements :as e]
    [toddler.elements.dropdown :as dropdown]
    [toddler.elements.multiselect :as multiselect]
    [toddler.elements.date :as date]
    [toddler.ui.default.color :refer [color]]
    [toddler.ui.provider :refer [ExtendUI UI]]
    ["toddler-icons$default" :as icon]))


(defstyled autosize-input AutosizeInput
  {:outline "none"
   :border "none"})


(defstyled buttons
  "div"
  {:display "flex"
   :button {:margin 0
            :border-radius 0}
   :button:first-of-type {:border-top-left-radius 4 :border-bottom-left-radius 4}
   :button:last-of-type {:border-top-right-radius 4 :border-bottom-right-radius 4}})

(defn button-colors
  [{:keys [context disabled]}]
  (let [context (if disabled :disabled context)]
    (case context
      ;;
      :positive
      {:color "white"
       :background-color (color :green/dark)
       :hover {:color "white"
               :background-color (color :green)}}
      ;;
      :negative
      {:color (color :red)
       :background-color (color :gray/light)
       :hover {:color "white"
               :background-color (color :red)}}
      ;;
      :fun
      {:color "white"
       :background-color (color :teal)
       :hover {:background-color (color :teal/saturated)}}
      ;;
      :fresh
      {:color (color :gray)
       :background-color (color :gray/light)
       :hover {:color "black"
               :background-color (color :yellow)}}
      ;;
      :stale
      {:color (color :gray)
       :background-color (color :gray/light)
       ::hover {:color "white"
                :background-color (color :gray/dark)}}
      ;;
      :disabled
      {:color "white"
       :background-color "#bbbbbb"
       :cursor "initial"}
      ;;
      {:color "white"
       :background-color "#5e82b8"
       :hover {:background-color (color :asphalt/dark)}})))


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
  (fn [{:keys [disabled] :as props}]
    (let [{:keys [background-color color hover]}
          (button-colors props)]
      (cond->
        {:color color 
         :background-color background-color 
         ":hover" (assoc hover :box-shadow "0px 2px 4px 0px #aeaeae")}
        disabled (assoc :pointer-events "none")))))


(defstyled checkbox e/Checkbox
  {:cursor "pointer"
   :path {:cursor "pointer"}
   :transition "color .2s ease-in"
   :width 20
   :height 20
   :border-radius 3
   :border-color "transparent"
   :padding 0
   :display "flex"
   :justify-content "center"
   :outline "none"
   :align-items "center"
   ":active" {:border-color "transparent"}}
  (fn [{:keys [theme value disabled]}]
    (let [[c bc] (case (:name theme)
                   ["white" (case value 
                              true (color :green)
                              false (color :disabled)
                              (color :gray/light))])]
      (cond->
        {:color c
         :background-color bc}
        disabled (assoc :pointer-events "none")))))


(defstyled row e/Row
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :flex-grow "1"
   ".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "14"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}}
  e/--flex-position)


(defstyled column e/Column
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"
   ".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "14"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}
   :padding 3}
  e/--flex-position)




(defstyled dropdown-wrapper "div"
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
  ($ ExtendUI
    {:components {:wrapper dropdown-wrapper
                  :option dropdown-option}}
    ($ dropdown/Popup
       {& props} 
       (c/children props))))


(defnc VanillaDropdownInput
  [props]
  ($ ExtendUI
    {:components {:input autosize-input
                  :wrapper "div"}}
    ($ dropdown/Input {& props})))


(defnc CalendarMonthDropdown
  [props]
  ($ UI
    {:components {:input VanillaDropdownInput
                  :popup DropdownPopup}}
    ($ date/CalendarMonthDropdown {& props})))


(defstyled calendar-month-dropdown CalendarMonthDropdown
  {:margin "5px 0"
   :cursor "pointer"
   :input {:cursor "pointer"
           :color (color :red)}})


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

(defstyled multiselect-wrapper
  "div"
  {:display "flex"
   :justify-content "row"
   :align-items "center"})


(defnc MultiselectInput
  [props]
  ($ UI
    {:components {:input autosize-input
                  :wrapper multiselect-wrapper}}
    ($ dropdown/Input
       {& props}
       (c/children props))))


(defstyled multiselect multiselect/Element
  {:display "flex"
   :alignItems "center"})


(defnc CalendarYearDropdown
  [props]
  ($ ExtendUI
    {:components
     {:input VanillaDropdownInput
      :popup DropdownPopup}}
    ($ date/CalendarYearDropdown {& props})))


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
  ($ UI
    {:components {:calendar/day calendar-day}}
    ($ date/CalendarWeek {& props})))


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
  ($ UI
    {:components {:header calendar-month-header
                  :calendar/week calendar-week}}
    ($ date/CalendarMonth
       {& props})))


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
  ($ date/TimestampCalendar {& props}))


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
  ($ ExtendUI
    {:components
     {:calendar timestamp-calendar
      :calendar/time timestamp-time
      :clear timestamp-clear
      :wrapper dropdown-wrapper}}
    ($ date/TimestampPopup
       {:ref _ref & props})))


(defnc PeriodElement
  [props]
  ($ ExtendUI
    {:components
     {:calendar timestamp-calendar
      :calendar/time timestamp-time
      :clear timestamp-clear}}
    ($ date/PeriodElement {& props})))


(defnc PeriodPopup
  [props _ref]
  {:wrap [react/forwardRef]}
  ($ ExtendUI
    {:components
     {:calendar timestamp-calendar
      :calendar/time timestamp-time
      :clear timestamp-clear
      :wrapper dropdown-wrapper}}
    ($ date/PeriodPopup
       {& props :ref _ref})))


(defstyled period-popup PeriodPopup
  {".period"
   {:display "flex"
    :flex-direction "row"}})


(def components
  {:row row
   :column column
   :checkbox checkbox
   :button button
   :buttons buttons
   :calendar/year-dropdown calendar-year-dropdown
   :calendar/month-dropdown calendar-month-dropdown
   :calendar/month calendar-month})
