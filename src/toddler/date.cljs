(ns toddler.date
  (:require
   [goog.string.format]
   [vura.core :as vura]
   [helix.core :refer [defhook]]
   [helix.hooks :as hooks]
   [toddler.core :as toddler]))

(defn ^:no-doc same [day1 day2]
  (and
   (every? some? [day1 day2])
   (= (select-keys day1 [:year :month :day-in-month])
      (select-keys day2 [:year :month :day-in-month]))))

(defhook use-week-days
  "Hook will return week days in short (three letter)
  format for current app/locale context value"
  []
  (toddler/use-calendar :weekdays/short))

(defhook use-calendar-months
  "Hook will return map with numeric months bound
  to month names. Month names will depend on app/locale
  context value."
  []
  (let [months (range 1 13)
        month-names (toddler/use-calendar :months)]
    (zipmap months month-names)))

(defn calendar-month
  "For given value of timestamp function will
  return days for month in form that at 6 weeks
  are included. First week will hold first day
  of month, than 4 weeks of that month and following
  weeks from next month.
  
  Maximal possible weeks is 6 for this logic"
  [value]
  (let [[first-day :as month] (vura/calendar-frame value :month)
        m (:month first-day)
        last-day (last month)
        first-week (vura/calendar-frame (:value first-day) :week)
        last-week (vura/calendar-frame (:value last-day) :week)
        f (:week first-day)
        l (:week last-day)
        middle (take-while
                #(not= l (:week %))
                (drop-while
                 (fn [x]
                   (<= (:week x) f))
                 month))
        days (concat (map #(assoc % :prev-month (not= m (:month %))) first-week)
                     middle
                     (map #(assoc % :next-month (not= m (:month %))) last-week))]
    (loop [days days]
      (if (> (count days) 40)
        days
        (recur
         (concat
          days
          (map
           #(assoc % :next-month true)
           (vura/calendar-frame
            (+ (:value (last days)) vura/day)
            :week))))))))

(defmulti reducer
  "Reducer used to work with calendar. You can exend current functionalities
  by implementing defmethod for topic of :type
  
  By default following is implemented:
  
   * :clear
   * :next-month
   * :prev-month
   * :next-year
   * :prev-year
   * :change-year
   * :change-month
   * :focus-value
   * :focus-date"
  (fn [_ {:keys [type]}] type))

(defmethod reducer :clear
  [_ _]
  {:calendar/position (-> (vura/date)
                          vura/time->value
                          vura/day-time-context
                          (assoc :hour 0
                                 :minute 0
                                 :second 0))})

(defmethod reducer :next-month
  [state _]
  (let [{{:keys [days-in-month] :as value} :calendar/position} state
        ;; Go to first day
        value (vura/context->value (assoc value :day-in-month 1))
        ;; And increase by length of this month
        value' (+ value (vura/days days-in-month))
        position (vura/day-time-context value')]
    (assoc state
      :calendar/position position
      :days (calendar-month value'))))

(defmethod reducer :prev-month
  [{value :calendar/position :as state} _]
  (let [value' (->
                value
                (assoc :day-in-month 1)
                vura/context->value
                (- vura/day))]
    (assoc state
      :calendar/position (vura/day-time-context value')
      :days (calendar-month value'))))

(defmethod reducer :next-year
  [{value :calendar/position :as state} _]
  (let [value' (->
                value
                (assoc :day-in-month 1)
                (update :year inc)
                vura/context->value)
        position (vura/day-time-context value')]
    (assoc state
      :calendar/position position
      :days (calendar-month value'))))

(defmethod reducer :prev-year
  [{value :calendar/position :as state} _]
  (let [value' (->
                value
                (assoc :day-in-month 1)
                (update :year dec)
                vura/context->value)
        position (vura/day-time-context value')]
    (assoc state
      :calendar/position position
      :days (calendar-month value'))))

(defmethod reducer :change-year
  [{value :calendar/position :as state} {:keys [year]}]
  (let [value' (->
                value
                (assoc :day-in-month 1
                       :year year)
                vura/context->value)
        position (vura/day-time-context value')]
    (assoc state
      :calendar/position position
      :days (calendar-month value'))))

(defmethod reducer :change-month
  [{value :calendar/position :as state} {:keys [month]}]
  (let [value' (->
                value
                (assoc :day-in-month 1
                       :month month)
                vura/context->value)
        position (vura/day-time-context value')]
    (assoc state
      :calendar/position position
      :days (calendar-month value'))))

(defmethod reducer :focus-value
  [state {:keys [value]}]
  (let [position-value (-> value
                           vura/day-time-context
                           (assoc :day-in-month 1)
                           vura/context->value)]
    (assoc state
      :calendar/position (vura/day-time-context position-value)
      :days (calendar-month position-value))))

(defmethod reducer :focus-date
  [state {:keys [date]}]
  (if-not date state
          (let [position-value (-> date
                                   vura/time->value
                                   vura/day-time-context
                                   (assoc :day-in-month 1)
                                   vura/context->value)]
            (assoc state
              :calendar/position (vura/day-time-context position-value)
              :days (calendar-month position-value)))))

(defhook use-calendar-month
  "Hook that will return state of calendar month and dispatch function.
  Based on either value or date it will create react state with
  toddler.date/reducer."
  ([{:keys [date value]}]
   (hooks/use-reducer
    reducer
    (let [value (or
                 value
                 (vura/time->value (or date (vura/date))))
          position (vura/day-time-context value)]
      {:calendar/position position
       :days (calendar-month value)}))))

(defhook use-calendar-days
  "Hook that is usefull when working with date selection. For given
  value and sequence of days it will return input days extended with
  keys: 
   
   * :picked - true if value is in this day
   * :today  - true if value is today"
  [value days]
  (if (nil? value) days
      (let [selected-context (vura/day-context (vura/time->value value))
            today (-> (vura/date)
                      vura/time->value
                      vura/day-time-context)]
        (map
         (fn [day]
           (cond-> day
             (same day selected-context) (assoc :picked true)
             (same day today) (assoc :today true)))
         days))))

(defhook use-period-callback
  "This hook will return fn that accepts day-time-context and
  computes next `[start end]` period value based on picked day-time-context
  value. If date is higher than end return value will be expanded end period.
  
  Same goes if input day-time-context is lower than start period.
  
  If day-time-context is inbetween it will narrow closer limit. Either start or end
  
  
  Start and End values should be js/Date"
  [[start end :as period]]
  (hooks/use-memo
    [start end]
    (fn [day]
      (let [start-value (when start (vura/time->value start))
            end-value (when end (vura/time->value end))
            s (-> day
                  vura/time->value
                  vura/midnight
                  vura/value->time)
            e (-> day
                  vura/time->value
                  vura/before-midnight
                  vura/value->time)]
        (if (or (nil? period) (every? nil? period))
          [s e]
          ;; Otherwise some value exists
          (let [day-value (:value day)
                day-date (vura/value->time day-value)]
            (cond
              ;; It is between start and end
              (<= start-value day-value end-value)
              (let [ld (- day-value start-value)
                    rd (- end-value day-value)]
                (cond
                  ;; If clicked on same day as start or end
                  (or
                   (< ld vura/day)
                   (< rd vura/day))
                  [s e]
                  ;; If closer to left
                  (< ld rd) [(-> day-value vura/midnight vura/value->time) end]
                  ;; If closer to right
                  (> ld rd) [start (-> day-value vura/before-midnight vura/value->time)]))
                 ;; It is lower than start
                 ;; and ther is no end, switch
              (and  (< day-value start-value) (nil? end))
              [(-> end-value vura/midnight vura/value->time)
               (-> day-date vura/before-midnight vura/value->time)]
                ;; It is lower than start but end
                ;; value exists
              (and (< day-value start-value) (some? end))
              [(-> day-value vura/midnight vura/value->time)
               end]
              ;; It is higher than end and start doesn't exist
              (and (> day-value end-value) (nil? start))
              [(-> end-value vura/midnight vura/value->time)
               (-> day-value vura/before-midnight vura/value->time)]
              ;; It is higher than end
              (> day-value end-value)
              [start (-> day-value vura/before-midnight vura/value->time)])))))))

(defhook use-period-days
  "Hook will process input days by comparing if 
  each of input days is in `[start end]` period.
  
  Result will extended days with following keys:

   * :today        - boolean
   * :selected     - true if in period
   * :period-start - true if day is same as start value day
   * :period-end   - true if day is same as end value day"
  [[start end] days]
  (if (or start end)
    (let [start-value (when start (vura/time->value start))
          end-value (when end (vura/time->value end))
          period-start (when start (vura/day-context start-value))
          period-end (when end (vura/day-context end-value))
          today (-> (vura/date)
                    vura/time->value
                    vura/day-time-context)]
      (map
       (fn [day]
         (assoc day
           :today (same today day)
           :selected (if (some? end-value)
                       (<= start-value (:value day) end-value)
                       (<= start-value (:value day)))
           :period-start (when period-start (same day period-start))
           :period-end (when period-end (same day period-end))))
       days))
    days))
