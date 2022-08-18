(ns toddler.i18n.hr
  (:require 
    [toddler.i18n.number :as number]
    [toddler.i18n.dictionary 
     :refer [calendar dictionary]]
    [tongue.core :as tongue]))

(def format-number (number/number-formatter :hr))

(def inst-strings
  {:weekdays-narrow ["P" "U" "S" "Č" "P" "S" "N"]
   :weekdays-short  ["Ned" "Uto" "Sri" "Čet" "Pet" "Sub" "Pon"]
   :weekdays-long   ["Nedjelja" "Ponedjeljak" "Utorak" "Srijeda" "Četvrtak" "Petak" "Subota"]
   :months-narrow   ["S" "V" "O" "T" "S" "L" "S" "K" "R" "L" "S" "P"]
   :months-short    ["Sij" "Velj" "Ožu" "Trav" "Svi" "Lip" "Srp" "Kol" "Ruj" "Lis" "Stu" "Pro"]
   :months-long     ["Siječanj" 
                     "Veljača" 
                     "Ožujak" 
                     "Travanj" 
                     "Svibanj" 
                     "Lipanj" 
                     "Srpanj" 
                     "Kolovoz" 
                     "Rujan" 
                     "Listopad" 
                     "Studeni" 
                     "Prosinac"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["BC" "AD"]
   :eras-long       ["Before Christ" "Anno Domini"]})


(def dict
  {;; Numbers
   :tongue/format-number format-number
   :count "{1} predmeta"
   ;; 
   :date-full (tongue/inst-formatter "{weekday-short} {day}. {month-long}, {year}" inst-strings)
   :datetime-full (tongue/inst-formatter "{weekday-short}  {day}. {month-short} {year} {hour24-padded}:{minutes-padded}" inst-strings)
   :date-short (tongue/inst-formatter "{day-padded}.{month-numeric-padded}.{year}" inst-strings)
   :datetime-short (tongue/inst-formatter "{day}.{month-numeric}.{year} {hour24-padded}:{minutes-padded}" inst-strings)
   :time-military (tongue/inst-formatter "{hour24-padded}{minutes-padded}" inst-strings)})


(do
  (swap! calendar assoc :hr inst-strings)
  (swap! dictionary assoc :hr dict)
  (println "Added Croatian dictionary..."))
