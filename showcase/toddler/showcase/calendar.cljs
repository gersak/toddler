(ns toddler.showcase.calendar
  (:require
   [helix.core :refer [$ defnc]]
   [vura.core :as vura]
   ; [helix.dom :as d]
   [toddler.ui :as ui]
   [toddler.ui.default :as default]
   [toddler.dev :as dev]))


(defnc Calendar
   []
   (let [today (vura/date)
         value (vura/date->value today)
         context (vura/day-time-context value) 
         [{:keys [week]} :as week-context] (vura/calendar-frame value :week)
         month-context (vura/calendar-frame value :month)]
      ($ default/Provider
         ($ ui/row
            {:label "Day"}
            ($ ui/calendar-day {& context}))
         ($ ui/row
            {:label "Week"}
            ($ ui/calendar-week {:week week :days week-context}))
         ($ ui/row
            {:label "Month"}
            ($ ui/calendar-month {:days month-context}))
         ($ ui/row
            {:label "Month dropdown"}
            ($ ui/calendar-month-dropdown))
         ($ ui/row
            {:label "Year dropdown"}
            ($ ui/calendar-year-dropdown)))))



(dev/add-component
   {:key ::calendar
    :name "Calendar"
    :render Calendar})
