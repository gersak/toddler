(ns toddler.i18n.en
  (:require 
    [toddler.i18n.number :as number]
    [toddler.i18n.dictionary 
     :refer [calendar dictionary]]
    [tongue.core :as tongue]))


(def format-number (number/number-formatter :en))


(def inst-strings
  {:weekdays-narrow ["S" "M" "T" "W" "T" "F" "S"]
   :weekdays-short  ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
   :weekdays-long   ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
   :months-long     ["January" 
                     "February" 
                     "March" 
                     "April" 
                     "May" 
                     "June" 
                     "July" 
                     "August" 
                     "September" 
                     "October" 
                     "November" 
                     "December"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["BC" "AD"]
   :eras-long       ["Before Christ" "Anno Domini"]})


(def dict
  {;; Numbers
   :tongue/format-number format-number
   :count "{1} items"
   ;; 
   :date-full     (tongue/inst-formatter "{weekday-short} {month-long} {day}, {year} " inst-strings)
   :datetime-full     (tongue/inst-formatter "{weekday-short} {month-short} {day}, {year} {hour24-padded}:{minutes-padded}" inst-strings)
   :date-short    (tongue/inst-formatter "{month-numeric}/{day}/{year-2digit}" inst-strings)
   :datetime-short    (tongue/inst-formatter "{month-numeric}/{day}/{year-2digit} {hour24-padded}:{minutes-padded}" inst-strings)
   :time-military (tongue/inst-formatter "{hour24-padded}{minutes-padded}" inst-strings)})


(do
  (swap! calendar update :en merge inst-strings)
  (swap! dictionary update :en merge dict)
  (println "Added English dictionary..."))
