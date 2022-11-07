(ns toddler.ui.default.elements
  (:require
    ["react" :as react]
    [clojure.string :as str]
    [shadow.css :refer [css]]
    [helix.core
     :refer [$ defnc]]
    [helix.dom :as d]
    [helix.children :as c]
    [toddler.input
     :refer [AutosizeInput
             IdleInput]]
    [toddler.ui :refer [forward-ref]]
    [toddler.avatar :as a]
    [toddler.dropdown :as dropdown]
    [toddler.multiselect :as multiselect]
    [toddler.date :as date]
    [toddler.scroll :as scroll]
    [toddler.ui.provider :refer [ExtendUI UI]]
    ["toddler-icons$default" :as icon]))


(defnc simplebar
  [{:keys [className shadow hidden] :as props
    :or {shadow #{}}}
   _ref]
  {:wrap (forward-ref)}
  (let [$default (css {:transition "box-shadow 0.3s ease-in-out"})
        $shadow-top (css {:box-shadow "inset 0px 11px 8px -10px #CCC"})
        $shadow-bottom (css {:box-shadow "inset 0px -11px 8px -10px #CCC"}) 
        $hidden (css ["& .simplebar-track" {:display "none"}])
        className (str/join
                    " "
                    (cond-> [className $default]
                      (shadow :top) (conj $shadow-top) 
                      (shadow :bottom) (conj $shadow-bottom)
                      hidden (conj $hidden)))]
    ($ scroll/_SimpleBar
       {:className className
        :ref _ref
        & (-> props
              (dissoc :shadow :className :class)
              (:style scroll/transform-style))}
       (c/children props))))


(defnc autosize-input
  [props]
  ($ AutosizeInput
    {:className (css
                  :border-0
                  :outline-none)
     & props}))


(defnc idle-input
  [props]
  ($ IdleInput
    {:className (css {:outline "none"
                      :border "none"})
     & props}))


(defnc buttons
  [props]
  (let [$wrapper (css
                   :display "flex"
                   :button {:m-0
                            :rounded-none}
                   :button:first-of-type {:border-top-left-radius "4px" :border-bottom-left-radius "4px"}
                   :button:last-of-type {:border-top-right-radius "4px" :border-bottom-right-radius "4px"})]
    (d/div
      {:className $wrapper}
      (c/children props))))


(defn button-color
  [{:keys [context disabled]}]
  (let [context (if disabled :disabled context)
        $positive
        (css :text-green-200
             :bg-green-600
             [:hover :text-white :bg-green-500])
        ;;
        $negative
        (css
          :text-red-600
          :bg-red-200
          [:hover :bg-red-400 :text-red-900])
        $fun
        (css 
          :bg-cyan-500
          :text-cyan-100
          [:hover :bg-cyan-400])
        ;;
        $fresh
        (css
          :text-yellow-900
          :bg-yellow-300
          [:hover :text-black :bg-yellow-400]) 
        ;;
        $stale
        (css
          {:color :gray
           :background-color :gray/light}
          [:hover {:background-color "#d9d9d9"}])
        ;;
        $disabled
        (css 
          {:color "white"
           :background-color "#bbbbbb"
           :cursor "initial"
           :pointer-events "none"
           :user-select "none"})
        ;;
        $default
        (css :text-teal-900
             :bg-cyan-400
             [:hover :bg-cyan-300])]
    (case context
      :positive $positive
      :negative $negative
      :fun $fun
      :fresh $fresh
      :stale $stale
      :disabled $disabled
      $default)))


(defnc button
  [{:keys [disabled] :as props}]
  (let [$color (button-color props) 
        $layout (css 
                  :flex
                  :border-2
                  :border-transparent
                  :rounded-sm
                  ; :px-18
                  ; :py-4
                  {:justify-content "center"
                   :padding "8px 18px"
                   :align-items "center"
                   :max-height "30px"
                   :min-width "80px"
                   :font-size "1em"
                   :line-height "1.33"
                   :text-align "center"
                   :vertical-align "center"
                   :margin "3px 2px"
                   :transition "box-shadow .3s ease-in,background .3s ease-in"
                   :cursor "pointer"}
                  ["&:hover" {:transition "background .3s ease-in"}]
                  ["&:focus" {:outline "none"}]
                  ["&:active" {:transform "translate(0px,2px)" :box-shadow "none"}])
        $shadow (css
                  ["&:hover" {:box-shadow "0px 2px 4px 0px #aeaeae"}])
        $disabled (css :pointer-events "none")]
    (d/button
      {:class (cond-> [$layout
                       $color
                       $shadow]
                disabled (conj $disabled))}
      (c/children props))))


(defnc checkbox
  [{:keys [value disabled] :as props}]
  (let [$layout (css
                  {:cursor "pointer"
                   :transition "color .2s ease-in"
                   :width "1.5em" 
                   :height "1.5em"
                   :border-radius "4px"
                   :border-color "transparent"
                   :padding "0px"
                   :display "flex"
                   :justify-content "center"
                   :outline "none"
                   :align-items "center"}
                  ["& path" {:cursor "pointer"}]
                  ["&:active" {:border-color "transparent"}])
        $active (css
                  :text-white
                  :bg-green-500)
        $inactive (css
                    :text-white
                    :bg-gray-400)
        $disabled (css :pointer-events "none")]
    (d/button
      {:class [$layout
               (if value $active $inactive)
               (when disabled $disabled)]
       & (dissoc props :value)}
      ($ (case value
           nil icon/checkboxDefault
           icon/checkbox)))))



(defnc row
  [{:keys [label position] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :text-gray-800
                  {:display "flex"
                   :flex-direction "row"
                   :align-items "center"
                   :flex-grow "1"}
                  ["& .label"
                   {:margin "4px 4px 4px 4px"
                    :padding-bottom "2px"
                    :text-transform "uppercase"
                    :font-size "1em"
                    :color :text-gray-800}])
        $start (css {:justify-content "flex-start"})
        $center (css {:justify-content "center"})
        $end (css {:justify-content "flex-end"})
        $explode (css {:justify-content "space-between"})
        $position (case position
                    :center $center
                    :end $end
                    :explode $explode
                    $start)]
    (d/div
      {:ref _ref
       :class [$layout
               $position]
       :position position}
      (when label
        (d/div
          {:className "label"}
          (d/label label)))
      (c/children props))))


(defnc column
  [{:keys [label position] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :text-gray-800
                  {:display "flex"
                   :flex-direction "column"
                   :align-items "center"
                   :flex-grow "1"}
                  ["& .label"
                   {:margin "4px 4px 4px 4px"
                    :padding-bottom "2px"
                    :text-transform "uppercase"
                    :font-size "1em"
                    :color :text-gray-800}])
        $start (css {:justify-content "flex-start"})
        $center (css {:justify-content "center"})
        $end (css {:justify-content "flex-end"})
        $explode (css {:justify-content "space-between"})
        $position (case position
                    :center $center
                    :end $end
                    :explode $explode
                    $start)]
    (d/div
      {:ref _ref
       :class [$layout
               $position]
       :position position}
      (when label
        (d/div
          {:className "label"}
          (d/label label)))
      (c/children props))))


(defnc dropdown-wrapper
  [{:keys [style] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :flex
                  :flex-col
                  :m-0
                  :p-2
                  :bg-white
                  :shadow-xl
                  :rounded-sm
                  :border-2
                  :border
                  :border-gray-100
                  {:box-shadow "0 11px 25px -5px rgb(0 0 0 / 9%), 0 4px 20px 0px rgb(0 0 0 / 14%)"}
                  ; {:box-shadow "0px 3px 10px -3px black"}
                  ["& .simplebar-scrollbar:before"
                   :bg-gray-100
                   :pointer-events-none
                   {:max-height "400px"}])]
    (d/div
      {:ref _ref
       :class [$layout]
       :style style}
      (c/children props))))


(defnc dropdown-option
  [props]
  (println "RENDERING DROPDOWN OPTION!!!!")
  (let [$layout (css
                  :flex
                  :justify-start
                  :items-center
                  :cursor-pointer
                  :text-gray-500
                  :rounded-sm
                  :bg-white
                  {:transition "color .2s ease-in,background-color .2s ease-in"
                   :padding "4px 6px 4px 4px"}
                  [:hover :text-gray-600 :bg-cyan-100]
                  ["&:last-child" {:border-bottom "none"}])]
    (d/div
      {:class [$layout]
       & props}
      (c/children props))))


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


(defnc calendar-month-dropdown
  [props]
  (let [$style (css
                 {:margin "5px 0"
                  :cursor "pointer"}
                 ["& input" :cursor-pointer :text-rose-700])]
    ($ UI
       {:components {:input VanillaDropdownInput
                     :popup DropdownPopup}}
       ($ date/CalendarMonthDropdown {:className $style & props}))))


(defnc calendar-year-dropdown
  [props]
  (let [$style (css
                 :mx-3
                 :cursor-pointer
                 ["& input" :cursor-pointer :text-rose-700])]
    ($ ExtendUI
       {:components
        {:input VanillaDropdownInput
         :popup DropdownPopup}}
       ($ date/CalendarYearDropdown {:className $style & props}))))


(defnc multiselect-option 
  [props]
  (let [$style (css
                 :text-white
                 :bg-cyan-500
                 :rounded-sm
                 :px-2
                 :py-1
                 {:margin "3px"
                  :display "flex"
                  :flex-direction "row"
                  :justify-content "start"
                  :align-items "center"
                  :font-size "1em"}
                 ["& svg" {:margin "0 5px"
                           :padding-right "3px"}]
                 ["& .remove:hover" :text-black]
                 ["& .remove"
                  :text-cyan-700
                  :cursor-pointer
                  :flex
                  :items-center
                  :justify-center
                  {:transition "color .2s ease-in"}]
                 ["& .remove path" :cursor-pointer])]
    ($ multiselect/Option
       {:className $style & props})))


(defnc multiselect-wrapper
  [props]
  (d/div
    {:class (css
              {:display "flex"
               :justify-content "row"
               :align-items "center"
               :min-height "2.3em"})}
    (c/children props)))


(defnc MultiselectInput
  [props]
  ($ UI
    {:components {:input autosize-input
                  :wrapper multiselect-wrapper}}
    ($ dropdown/Input
       {& props}
       (c/children props))))


(defnc multiselect
  [props]
  (d/div
    {:class (css
              {:display "flex"
               :alignItems "center"})}
    ($ multiselect/Element
       {& props})))


(defnc calendar-day
  [props]
  (let [$style (css
                 :border
                 :border-transparent
                 :text-gray-500
                 ;;
                 ["& .day"
                  {:text-align "center"
                   :font-size "0.8em"
                   :user-select "none"
                   :padding "3px"
                   :width "25px"
                   :border-collapse "collapse"
                   :border "1px solid transparent"
                   :cursor "pointer"}]
                 ["& .day.today"
                  :border
                  :border-teal-600
                  :text-gray-800
                  :bg-cyan-300]
                 ;;
                 ["& .day.weekend" :text-rose-700]
                 ;;
                 ["& .day.empty" {:cursor "default"}]
                 ;;
                 ["& .day.disabled, & .day:hover.disabled"
                  :border
                  :border-solid
                  :border-transparent
                  :text-gray-500
                  :cursor-default]
                 ;;
                 ["& .day:hover:not(.empty), & .day.selected"
                  :text-white
                  :border
                  :rounded-sm
                  :border-cyan-800
                  :bg-cyan-500
                  :font-bold])]
    ($ date/CalendarDay
       {:className $style
        & (dissoc props :className :class)})))


(defnc calendar-week
  [props]
  (let [$style (css
                 ["& .week-days"
                  :flex
                  :flex-row])]
    ($ UI
       {:components {:calendar/day calendar-day}}
       ($ date/CalendarWeek
          {:className $style
           & props}))))


(defnc calendar-month-header
  [props]
  (let [$style (css
                 :flex
                 :flex-row
                 :cursor-default
                 {:cursor "default"}
                 ["& .day-wrapper .day" :text-gray-500]
                 ["& .day.weekend" :text-rose-700]
                 ["& .day-wrapper"
                  {:border-collapse "collapse"
                   :border "1px solid transparent"}]
                 ["& .day-wrapper .day"
                  {:text-align "center"
                   :font-weight "500"
                   :font-size "0.8em"
                   :border-collapse "collapse"
                   :user-select "none"
                   :padding "3px"
                   :width "25px"
                   :border "1px solid transparent"}])]
    ($ date/CalendarMonthHeader
       {:className $style & props})))


(defnc CalendarMonth
  [props]
  ($ UI
    {:components
     {:header calendar-month-header
      :calendar/week calendar-week}}
    ($ date/CalendarMonth
       {& props})))


(defnc calendar-month
  [props]
  (let [$style (css
                 :flex
                 :flex-col
                 {:width "220px"})]
    ($ CalendarMonth
       {:className $style
        & props})))


(defnc timestamp-calendar
  [props]
  (let [$style (css
                 {:display "flex"
                  :flex-direction "column"
                  :border-radius "3px"
                  :padding "7px"
                  :width "230px"
                  :height "190px"}
                 ; (str popup/dropdown-container) {:overflow "hidden"}
                 ["& .header-wrapper" {:display "flex" :justify-content "center" :flex-grow "1"}]
                 ["& .header"
                  {:display "flex"
                   :justify-content "space-between"
                   :width "200px"
                   :height "38px"}]
                 ["& .header .years"
                  {:position "relative"
                   :display "flex"
                   :align-items "center"}]
                 ["& .header .months"
                  {:position "relative"
                   :display "flex"
                   :align-items "center"}]
                 ["& .content-wrapper"
                  {:display "flex"
                   :height "150px"
                   :justify-content "center"
                   :flex-grow "1"}])]
    ($ date/TimestampCalendar
       {:className $style
        & props})))


(defnc timestamp-time
  [props]
  ($ date/TimestampTime
    {:className (css
                  {:display "flex"
                   :justify-content "center"
                   :align-items "center"

                   :font-size "1em"
                   :margin "3px 0 5px 0"
                   :justify-self "center"}
                  ["& input" {:max-width "40px"}]
                  ["& .time" {:outline "none"
                              :border "none"}])
     & props}))


(defnc timestamp-clear
  [props]
  ($ date/TimestampClear
    {:className (css
                  :flex
                  :bg-gray-400
                  :text-white
                  :items-center
                  :justify-center
                  :cursor-pointer
                  {:width "15px"
                   :height "15px"
                   :padding "4px"
                   :justify-self "flex-end"
                   :transition "background .3s ease-in-out"
                   :border-radius "20px"}
                  ["&:hover" :bg-red-500])
     & props}))


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


(defnc period-popup
  [props _ref]
  {:wrap [react/forwardRef]}
  (let [$style (css ["& .period" :flex :flex-row])]
    ($ ExtendUI
       {:components
        {:calendar timestamp-calendar
         :calendar/time timestamp-time
         :clear timestamp-clear
         :wrapper dropdown-wrapper}}
       ($ date/PeriodPopup
          {:className $style
           & props :ref _ref}))))


(defnc avatar
  [{:keys [size className] :as props}]
  (let [size' (case size
                :small "1.5em"
                :medium "3.5em"
                :large "10em"
                size)]
    ($ a/Avatar
       {:className className
        :style {:border-radius 20
                :width size'
                :height size'}
        & props})))


(defnc tooltip
  [props]
  (c/children props))


(defnc card-action
  [{:keys [tooltip disabled onClick context] :as props}]
  ($ tooltip
    {:message tooltip
     :disabled (or (empty? tooltip) disabled)}
    (let [$wrapper (css
                     {:height "32px"
                      :width "32px"
                      :display "flex"
                      :justify-content "center"
                      :align-items "center"
                      :border-radius "32px"})
          $action (css
                    {:width "26px"
                     :height "26px"
                     :border-radius "26px"
                     :cursor "pointer"
                     :transition "color,background-color .2s ease-in-out"
                     :display "flex"
                     :color "#929292"
                     :justify-content "center"
                     :align-items "center"}
                    ["& svg" {:height "14px"
                              :width "14px"}])
          $default (css
                     [:hover :bg-cyan-500
                      {:color "#fff8f3"}])
          $negative (css
                      [:hover :bg-red-500
                       {:color "#fff8f3"}])]
      (d/div
        {:class [$wrapper
                 $action
                 (case context
                   :negative $negative
                   $default)]}
        (d/div
          {:className "action"
           :onClick onClick}
          (c/children props))))))


(defnc card-actions
  [props]
  (let [$layout (css
                  {:position "absolute"
                   :top "-16px"
                   :right "-16px"}
                  ["& .wrapper"
                    {:display "flex"
                     :flex-direction "row"
                     :justify-content "flex-end"
                     :align-items "center"}])]
    (d/div
      {:className ["card-actions" $layout]}
      (d/div
        {:className "wrapper"}

        (c/children props)))))


(defnc card
  [props]
  (let [$card (css
                {:position "relative"
                 :display "flex"
                 :flex-direction "column"
                 :max-width "300px"
                 :min-width "180px"
                 :padding "10px 10px 5px 10px"
                 :background-color "#eaeaea"
                 :border-radius "5px"
                 :transition "box-shadow .2s ease-in-out"}
                ["& .card-actions" {:opacity "0"
                                   :transition "opacity .4s ease-in-out"}]
                ["&:hover .card-actions" {:opacity "1"}]
                ;;
                ["& .avatar"
                 {:position "absolute"
                  :left "-10px"
                  :top "-10px"
                  :transition "all .1s ease-in-out"}]
                ["&:hover" {:box-shadow "1px 4px 11px 1px #ababab"}])]
    (d/div
      {:className $card}
      (c/children props))))



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
     :calendar/day calendar-day
     :calendar/week calendar-week
     ; :calendar/time calendar-time
     :calendar/year-dropdown calendar-year-dropdown
     :calendar/month-dropdown calendar-month-dropdown
     :calendar/month calendar-month}
    #:input {:autosize autosize-input
             :idle idle-input}))
