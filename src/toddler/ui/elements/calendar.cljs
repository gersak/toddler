(ns toddler.ui.elements.calendar
  (:require
   [vura.core :as vura]
   [shadow.css :refer [css]]
   [helix.core
    :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.material.outlined :as outlined]
   [toddler.date :as date]))

(def $calendar-month-header
  (css
   :flex
   :flex-row
   :cursor-default
   {:cursor "default"}
   ["& .day-wrapper"
    :h-6 :w-10 :flex :justify-center :items-center
    {:border-collapse "collapse"
     :border "1px solid transparent"}]
   ["& .day-wrapper .day"
    :text-normal
    :uppercase :select-none
    :font-medium
    {:font-size "0.625rem"
     :line-height "1.125rem"}]))

(def $calendar-month
  (css
   :flex
   :flex-col
   :items-center))

(def $calendar-week
  (css :flex :flex-row :mt-2 :px-2))

(def $calendar-day
  (css
    ;;
   :color-inactive
    ;;
   ["& .day"
    :w-10 :h-6 :flex :items-center :justify-center
    :text-normal :cursor-pointer :select-none :font-medium
    {:font-size "0.75rem"
     :font-weight "500"
     :line-height "1.25rem"}]
    ;;
   ["& .day.empty" {:cursor "default"}]
    ;;
   ["& .day.disabled, & .day:hover.disabled"
    :border
    :border-solid
    :border-transparent
    :color-inactive
    :cursor-default]
    ;;
   ["&:hover:not(.empty):not(.period-start):not(.period-end):not(.picked) .day"
    {:border-color "#var(--cd-hover-border) !important"
     :color "var(--cd-hover-color)"}]
    ;;
   ["&.selected .day"
    {:border-top "1px solid var(--cd-selected-border)"
     :border-bottom "1px solid var(--cd-selected-border)"
     :color "var(--cd-selected-color) !important"}]
   ["&.picked .day"
    {:color "var(--cd-selected-color)"
     :border-left "1px solid var(--cd-selected-border)"
     :border-right "1px solid var(--cd-selected-border)"
     :border-top "1px solid var(--cd-selected-border)"
     :border-bottom "1px solid var(--cd-selected-border)"
     :border-top-left-radius "3px"
     :border-bottom-left-radius "3px"
     :border-top-right-radius "3px"
     :border-bottom-right-radius "3px"}]
    ;;
   ["&.prev-month:not(.period-start):not(.period-end):not(.picked) .day, &.next-month .day:not(.period-end):not(.picked)"
    {:color "var(--cd-other-color)"}]
    ;;
   ["&.selected:first-child .day, &.period-start .day"
    {:border-left "1px solid var(--cd-selected-border) "
     :border-top-left-radius "3px"
     :border-bottom-left-radius "3px"}]
    ;;
   ["&.period-start .day"
    {:border-right "1px solid var(--cd-selected-border)"
     :background-color "var(--cd-period-marker-bg)"
     :border-top-left-radius "3px"
     :border-bottom-left-radius "3px"
     :color "var(--cd-period-marker-color)"}]
    ;;
   ["&.selected:last-child .day, &.period-end .day"
    {:border-right "1px solid var(--cd-selected-border) "
     :border-top-right-radius "3px"
     :border-bottom-right-radius "3px"}]
   ["&.period-end .day"
    {:border-left "1px solid var(--cd-selected-border)"
     :background-color "var(--cd-period-marker-bg) "
     :color "var(--cd-period-marker-color)"
     :border-top-right-radius "3px"
     :border-bottom-right-radius "3px"}]))

(def $calendar
  (css
   {:display "flex"
    :flex-direction "column"
    :background-color "transparent"}
   ; :border
   ; :border-normal
   :rounded-sm
   :py-2
    ; (str popup/dropdown-container) {:overflow "hidden"}
   ["& .header-wrapper"
    :flex :flex-grow]
   ["& .header"
    :flex
    :grow
    :h-6
    :mb-2
    :px-2
    :justify-between
    :text-normal]
   ["& .header" :select-none]
   ["& .header svg"
    :border :border-transparent
    {:transition "border-color .3s ease-in-out, background-color .3s ease-in-out, color .3s ease-in-out"
     :color "var(--calendar-button-color)"}]
   ["& .header svg:hover"
    :rounded-sm
    {:background-color "var(--calendar-button-bg-hover)"
     :color "var(--calendar-button-color-hover)"}]
   ["& .header .back, & .header .forward" :flex]
   ["& .header .info" :flex :justify-center :items-center :px-2]
   ["& .header .info .month" :uppercase
    {:font-weight "600"
     :font-size "0.75rem"
     :line-height "1.25rem"}]
   ["& .header .info .year"
    :ml-2
    {:font-weight "600"
     :font-size "0.75rem"
     :line-height "1.25rem"}]
   ["& .header svg" :w-6 :h-6 :cursor-pointer]
   ["& .header svg:hover" :text-hover]
   ["& .header .info" :grow]

   ["& .header .months"
    :select-none :flex
    :relative :items-center]
   ["& .content-wrapper"
    {:display "flex"
     :justify-content "center"
     :flex-grow "1"}]))

(defnc calendar-month-header
  [{:keys [className days]}]
  (let [week-days (date/use-week-days)
        day-names (zipmap
                   [7 1 2 3 4 5 6]
                   week-days)]
    (d/div
     {:class [className $calendar-month-header]}
     (map
      (fn [n]
        (let [is-weekend (vura/*weekend-days* n)]
          (d/div
           {:class "day-wrapper"
            :key n}
           (d/div
            {:class (cond-> ["day"]
                      is-weekend (conj "weekend"))}
            (get day-names n)))))
      days))))

(defnc calendar-day
  [{:keys [value
           onClick
           class
           className]
    {:keys [day-in-month
            today
            first-day-in-month?
            last-day-in-month?
            disabled
            next-month
            prev-month
            period-start
            period-end
            picked
            selected
            weekend?]} :day}]
  (d/div
   {:onClick (when-not disabled onClick)
    :class (cond-> []
             (string? className) (conj className)
             (string? class) (conj class)
             (sequential? class) (into class)
             period-start (conj "period-start")
             period-end (conj "period-end")
             picked (conj "picked")
             next-month (conj "next-month")
             prev-month (conj "prev-month")
             selected (conj "selected")
             first-day-in-month? (conj "first-day-in-month")
             last-day-in-month? (conj "last-day-in-month")
             disabled (conj "disabled")
             today (conj "today")
             weekend? (conj "weekend")
             (nil? value) (conj "empty"))}
   (d/div
    {:class "day"}
    (d/div (or day-in-month " ")))))

(defnc calendar-month
  [{:keys [days on-select disabled read-only]}]
  (let [weeks (partition 7 days)]
    (d/div
     {:class $calendar-month}
     ($ calendar-month-header {:days (range 1 8)})
     (map
      (fn [[{w :week} :as days]]
        (d/div
         {:key w
          :className $calendar-week}
         (map
          (fn [day]
            ($ calendar-day
               {:key [(:month day) (:day-in-month day)]
                :day day
                :className $calendar-day
                :onClick (fn []
                           (when (and (not disabled) (not read-only))
                             (when (fn? on-select)
                               (on-select day))))}))
          days)))
      weeks))))

(defnc calendar*
  [{:keys [year month days on-select dispatch]}]
  (let [search-fn (date/use-calendar-months)]
    (d/div
     {:class [$calendar "calendar"]}
     (d/div
      {:className "header-wrapper"}
      (d/div
       {:className "header"}
       (d/div
        {:className "back"}
        ($ outlined/keyboard-double-arrow-left {:onClick #(dispatch {:type :prev-year})})
        ($ outlined/keyboard-arrow-left {:onClick #(dispatch {:type :prev-month})}))
       (d/div
        {:className "info"}
        (d/div {:className "month"} (search-fn month))
        (d/div {:className "year"} year))
       (d/div
        {:className "forward"}
        ($ outlined/keyboard-arrow-right {:onClick #(dispatch {:type :next-month})})
        ($ outlined/keyboard-double-arrow-right {:onClick #(dispatch {:type :next-year})}))))
     (d/div
      {:className "content-wrapper"}
      (d/div
       {:className "content"}
       ($ calendar-month
          {:days days
           :on-select on-select}))))))

(defnc calendar
  [{:keys [value onChange on-change]}]
  (let [[{:keys [days]
          {:keys [month year]} :calendar/position} dispatch]
        (date/use-calendar-month {:date value})
        ;;
        on-change (or on-change onChange)
        days (date/use-calendar-days value days)]
    ($ calendar*
       {:year year
        :month month
        :days days
        :dispatch dispatch
        :on-select (fn [{:keys [value]}]
                     (when (ifn? on-change)
                       (on-change (vura/value->time value))))})))

(defnc period-calendar
  [{:keys [onChange on-change]
    [start end :as period] :value}]
  (let [on-change (or onChange on-change)
        period-change (date/use-period-callback [start end])
        ;;
        [{:keys [days]
          {:keys [year month]} :calendar/position} dispatch]
        (date/use-calendar-month {:date (or start end)})
        ;;
        days (date/use-period-days period days)]
    ;;
    (hooks/use-effect
      [start end]
      (when (nil? period)
        (dispatch
         {:type :focus-date
          :date (or start end)})))
    ;;
    ($ calendar*
       {:year year
        :month month
        :days days
        :dispatch dispatch
        :on-select #(when (ifn? on-change)
                      (on-change (period-change %)))})))

