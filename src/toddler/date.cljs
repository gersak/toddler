(ns toddler.date
  (:require
    ["react" :as react]
    [clojure.string :as str]
    [goog.string :as gstr]
    [goog.string.format]
    [vura.core :as vura]
    [helix.core :refer [$ defnc defhook create-context provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.styled-components :refer [defstyled]]
    [helix.children :as c]
    [toddler.ui :as ui]
    [toddler.hooks :as th]
    [toddler.mask :refer [use-mask]]
    [toddler.dropdown :as dropdown]
    [toddler.popup :as popup]
    [toddler.input :refer [AutosizeInput]]
    ["toddler-icons$default" :as icon]))


(defstyled autosize-input AutosizeInput
  {:outline "none"
   :border "none"})

;; CALENDAR
(def ^:dynamic ^js *calendar-events* (create-context))
(def ^:dynamic ^js *calendar-selected* (create-context))
(def ^:dynamic ^js *calendar-disabled* (create-context))
(def ^:dynamic ^js *calendar-opened* (create-context))
(def ^:dynamic ^js *calendar-state* (create-context))


(defhook use-calendar-events [] (hooks/use-context *calendar-events*))
(defhook use-calendar-state [] (hooks/use-context *calendar-state*))


(defn same [day1 day2]
  (and
    (every? some? [day1 day2]) 
    (= (select-keys day1 [:year :month :day-in-month])
       (select-keys day2 [:year :month :day-in-month]))))


(defnc CalendarDay
  [{:keys [value
           day-in-month
           today
           className
           onClick]
    :as props}]
  (let [is-weekend (cond (some? value)
                         (if (vura/weekend? value)
                           true false))
        is-disabled (hooks/use-context *calendar-disabled*)
        disabled (when (ifn? is-disabled) (is-disabled props))
        is-selected (hooks/use-context *calendar-selected*)
        selected (when (ifn? is-selected) (is-selected props))
        #_is-holiday #_(cond (some? value)
                             (if (-> value vura/*holiday?*)
                               "red" ""))]
    (d/div
      {:class className
       :onClick (when-not disabled onClick)}
      (d/div
        {:class (cond-> ["day"]
                  selected (conj "selected")
                  disabled (conj "disabled")
                  today (conj "today")
                  is-weekend (conj "weekend")
                  (nil? value) (conj "empty"))}
        (d/div (or day-in-month " "))))))


(defhook use-week-days [] (th/use-calendar :weekdays/short))


(defhook use-calendar-months
  []
  (let [months (range 1 13)
        month-names (th/use-calendar :months)]
    (zipmap months month-names)))


(defhook use-calendar-years
  []
  (let [year (vura/year? (vura/date))]
    (range (- year 5) (+ year 5))))


(defhook use-timestamp-events
  [{:keys [day-in-month year selected] :as timestamp} set-timestamp!]
  (hooks/use-memo
   [timestamp]
   (let [timestamp (if (nil? timestamp)
                     (-> (vura/date) vura/time->value vura/midnight vura/day-time-context)
                     timestamp)]
     {:on-clear #(set-timestamp! nil)
      :on-next-month
      (fn []
        (let [{:keys [day-in-month days-in-month]} timestamp
              value (vura/context->value (assoc timestamp :day-in-month 1))
              value' (+ value (vura/days days-in-month))
              {:keys [days-in-month] :as timestamp'} (vura/day-time-context value')]
          (set-timestamp! (assoc timestamp'
                                 :day-in-month (min day-in-month days-in-month)
                                 :selected selected))))
       ;;
      :on-prev-month
      (fn []
        (let [{:keys [day-in-month]} timestamp
              value (vura/context->value (assoc timestamp :day-in-month 1))
              value' (- value (vura/days 1))
              {:keys [days-in-month] :as timestamp'} (vura/day-time-context value')]
          (set-timestamp! (assoc timestamp'
                                 :day-in-month (min day-in-month days-in-month)
                                 :selected selected))))
       ;;
      :on-day-change
      (fn [day-in-month]
        (set-timestamp!
         (assoc
          timestamp
          :selected (-> timestamp
                        (assoc :day-in-month day-in-month)
                        vura/context->value)
          :day-in-month day-in-month)))
      ;;
      :on-year-change (fn [y]
                        (set-timestamp! (assoc timestamp :year y)))
      :on-month-change (fn [month]
                         (let [v (vura/utc-date-value year month)
                               {last-day :days-in-month} (vura/day-context v)]
                           (set-timestamp!
                             (assoc timestamp
                                    :month month
                                    :day-in-month (min day-in-month last-day)))))
      :on-time-change #(set-timestamp! (merge timestamp %))})))


(defhook use-calendar
  ([props] (use-calendar props :month))
  ([{:keys [disabled read-only onChange]
     upstream-value :selected
     :or {onChange identity}}
    period]
    (let [[{:keys [value selected] :as state} set-state!] (hooks/use-state nil)
          ;;
          set-state! (hooks/use-memo
                       [disabled read-only]
                       (if (or disabled read-only)
                         (fn [& _])
                         set-state!))
          events (use-timestamp-events state set-state!)
          days (if-not selected (vura/calendar-frame value period)
                 (let [selected (-> selected vura/day-time-context)]
                   (map
                     (fn [day]
                       (if-not (same day selected) day
                         (assoc day :selected true)))
                     (vura/calendar-frame value period))))]
      (hooks/use-effect
        [state]
        (let [nv (vura/context->value state)]
          (when-not (= nv value)
            (set-state! assoc :value nv))))
      (hooks/use-effect
        [upstream-value]
        ;; Override current state when upstream value changes
        (if (some? upstream-value)
          (when-not (= upstream-value selected)
            (set-state! (-> upstream-value
                            vura/day-time-context
                            (assoc :selected upstream-value))))
          (let [today (-> (vura/date)
                          vura/time->value
                          vura/day-time-context
                          (assoc :hour 0
                                 :minute 0
                                 :second 0))]
            (set-state! today))))
      (hooks/use-effect
        [selected]
        (when-not (= selected upstream-value)
          (onChange selected)))
      {:state state
       :days days
       :events events})))
