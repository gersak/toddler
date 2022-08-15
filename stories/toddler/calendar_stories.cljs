(ns toddler.calendar-stories
  (:require
    [vura.core :as vura]
    [helix.core :refer [$]]
    toddler.theme
    [toddler.interactions :as interactions]))



(def ^:export default
  #js {:title "Toddler Calendar"})


(defn ^:export CalendarDay
  []
  ($ interactions/calendar-day
     {& (-> (vura/date)
            vura/time->value
            vura/day-time-context)}))


(defn ^:export CalendarMonthHeader
  []
  ($ interactions/calendar-month-header
     {:days [7 1 2 3 4 5 6]}))


(defn ^:export CalendarMonth
  []
  ($ interactions/calendar-month
     {:days (vura/calendar-frame
              (->
                (vura/date)
                vura/date->value)
              :month)}))
