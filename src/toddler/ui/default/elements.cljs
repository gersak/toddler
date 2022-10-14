(ns toddler.ui.default.elements
  (:require
    ["react" :as react]
    [clojure.string :as str]
    [helix.core
     :refer [$ defnc]]
    [helix.styled-components :refer [defstyled]]
    [helix.children :as c]
    [toddler.input
     :refer [AutosizeInput
             IdleInput]]
    [toddler.elements :as e]
    [toddler.layout :as l]
    [toddler.avatar :as a]
    [toddler.dropdown :as dropdown]
    [toddler.multiselect :as multiselect]
    [toddler.date :as date]
    [toddler.scroll :refer [SimpleBar]]
    [toddler.ui.default.color :refer [color]]
    [toddler.ui.provider :refer [ExtendUI UI]]))


(defstyled simplebar SimpleBar
  {"transition" "box-shadow 0.3s ease-in-out"}
  (fn 
    [{:keys [$shadow-top $shadow-bottom $hidden]}]
    (let [box-shadow (cond-> []
                       $shadow-top (conj "inset 0px 11px 8px -10px #CCC")
                       $shadow-bottom (conj "inset 0px -11px 8px -10px #CCC"))]
      (cond-> nil
        (not-empty box-shadow)
        (assoc :box-shadow (str/join ", " box-shadow))
        $hidden
        (assoc ".simplebar-track" {:display "none"})))))


(defstyled autosize-input AutosizeInput
  {:outline "none"
   :border "none"})


(defstyled idle-input IdleInput
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
   :font-size "1em"
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


(defstyled row l/Row
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :flex-grow "1"
   ".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "1em"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}}
  l/--flex-position)


(defstyled column l/Column
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"
   ".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "1.2em"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}
   :padding 3}
  l/--flex-position)




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
  {:font-size "1em"
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
   :padding "5px 5px"
   :font-size "1em"
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
   :align-items "center"
   :min-height 35})


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
    :font-size "0.8em"
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
   :font-size "1em"})


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
     :font-size "1em"
     :border-collapse "collapse"
     :user-select "none"
     :padding 3
     :width 20
     :border "1px solid transparent"}}
   ".day-wrapper .day"
   {:color (color :gray)
    :font-size "1em"
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
   :font-size "1em"
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


(defstyled avatar a/Avatar
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


(defstyled small-avatar a/Avatar
  {:border-radius 20
   :width 20
   :height 20})


(defstyled medium-avatar a/Avatar
  {:border-radius 20
   :width 36
   :height 36})

(defstyled large-avatar a/Avatar
  {:border-radius 20
   :width 144
   :height 144})


(let [size 32
      inner 26
      icon 14]
  (defstyled card-action e/CardAction
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
    (fn [{:keys [context]}]
      {:background-color (color :white)
       ".action:hover"
       (case context
         :negative
         {:background-color (color :red)
          :color "#fff8f3"}
         {:background-color (color :teal)
          :color "#fff8f3"})
       ".action"
       {:background-color "#929292"
        :color (color :gray/light)}})))


(defstyled card-actions e/CardActions
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



(def components
  (merge
    {:row row
     :card card
     :card/action card-action
     :card/actions card-actions
     :identity identity
     :avatar avatar
     :column column
     :checkbox checkbox
     :button button
     :buttons buttons
     :simplebar simplebar
     :calendar/year-dropdown calendar-year-dropdown
     :calendar/month-dropdown calendar-month-dropdown
     :calendar/month calendar-month}
    #:input {:autosize autosize-input
             :idle idle-input}))
