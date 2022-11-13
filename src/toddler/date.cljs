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
(def ^:dynamic ^js *calendar-disabled* (create-context (constantly false)))
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
           selected
           disabled
           className
           onClick]}]
  (let [is-weekend (cond (some? value)
                         (if (vura/weekend? value)
                           true false))
        #_is-holiday #_(cond (some? value)
                             (if (-> value vura/*holiday?*)
                               "red" ""))]
    (d/div
      {:class className
       :onClick onClick}
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


(defnc TimestampPopup
  [{:keys [year month day-in-month hour minute className]} popup]
  {:wrap [(react/forwardRef)]}
  (let [opened (hooks/use-context *calendar-opened*)]
    (when opened
      ($ popup/Element
         {:ref popup
          :className className 
          :wrapper ui/wrapper
          :preference popup/cross-preference}
         ($ ui/calendar
            {:year year
             :month month
             :day-in-month day-in-month})
         (d/div
           {:style
            {:display "flex"
             :flex-grow "1"
             :justify-content "center"}}
           ($ ui/calendar-time
              {:hour hour
               :minute minute})
           ($ ui/clear))))))


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


(defnc TimestampDropdown
  [{:keys [value
           onChange
           disabled
           read-only]
    :as props}]
  (let [[{:keys [selected] :as state} set-state!] (hooks/use-state nil)
        ;;
        [opened set-opened!] (hooks/use-state false)
        popup (hooks/use-ref nil)
        ;;
        area (hooks/use-ref nil)
        ;;
        set-state! (hooks/use-memo
                     [disabled read-only]
                     (if (or disabled read-only)
                       (fn [& _])
                       set-state!))
        selected? (hooks/use-memo
                    [selected]
                    (let [{:keys [day-in-month year month]} (some->
                                                              selected
                                                              (vura/time->value)
                                                              (vura/day-time-context))]
                      (fn [props]
                        (=
                          (select-keys
                            props
                            [:day-in-month :year :month])
                          {:year year
                           :day-in-month day-in-month
                           :month month}))))
        events (use-timestamp-events state set-state!)
        cache (hooks/use-ref state)]
    ;; TODO - timestamp should be a little more complex than this
    ;; It shouldn't have single popup props since those props are used
    ;; to navigate calendar. Calendar itself should have props that are
    ;; responsible for calendar navigation, but that affects use-calendar-events
    ;; hook. Point is that two states are required. One for calendar and one for
    ;; current value context (currently implemented)
    ;; Good enough for now lets move on!
    (hooks/use-effect
      [selected]
      (if (some? selected)
        (when-not (= selected value)
          (let [ov (-> state
                       vura/context->value
                       vura/value->time)]
            (when-not (= value ov)
              (-> value
                  vura/time->value
                  vura/day-time-context
                  set-state!))))
        (set-state! nil)))
    ;;
    (popup/use-outside-action
      opened area popup
      (fn [e]
        ;; FIXME  - Don't know how to do this more elegant
        ;; This will prevent updating when another dropdown
        ;; is event target
        (when (.contains js/document.body (.-target e))
          (set-opened! false)
          (when (and (ifn? onChange) (not= @cache value))
            (onChange @cache)))))
    ;;
    (hooks/use-effect
      [state]
      (if state
        (reset! cache (-> state vura/context->value vura/value->date))
        (reset! cache nil)))
    ;;
    (provider
      {:context *calendar-selected*
       :value selected?}
      (provider
        {:context *calendar-events*
         :value events}
        (provider
          {:context *calendar-disabled*
           :value disabled}
          (provider
            {:context *calendar-opened*
             :value opened}
            ($ popup/Area
               {:ref area}
               ($ ui/field
                  {:open set-opened!
                   :opened opened
                   & props})
               (when (and (not read-only) (not disabled) opened)
                 ($ ui/popup {:ref popup & state})))))))))

;; PERIOD

(defnc PeriodElement
  [{:keys [className]}]
  {:wrap [(react/forwardRef)]}
  (let [{start-events :start
         end-events :end} (use-calendar-events)
        [start end] (use-calendar-state)]
    (d/div
     {:className className}
     (provider
      {:context *calendar-events*
       :value start-events}
      (d/div
       {:className "start"}
       ($ ui/calendar
          {:year (:year start)
           :month (:month start)
           :day-in-month (:day-in-month start)})
       (d/div
         {:style
          {:display "flex"
           :flex-grow "1"
           :justify-content "center"}}
         ($ ui/calendar-time
            {:hour (:hour start)
             :minute (:minute start)})
         ($ ui/clear))))
     (provider
      {:context *calendar-events*
       :value end-events}
      (d/div
        {:className "end"}
        ($ ui/calendar
           {:year (:year end)
            :month (:month end)
            :day-in-month (:day-in-month end)})
        (d/div
          {:style
           {:display "flex"
            :flex-grow "1"
            :justify-content "center"}}
          ($ ui/calendar-time
             {:hour (:hour end)
              :minute (:minute end)})
          ($ ui/clear)))))))


(defnc PeriodPopup
  [{:keys [className]
    :as props}
   popup]
  {:wrap [(react/forwardRef)]}
  ($ popup/Element
     {:ref popup
      :className className 
      :preference popup/cross-preference}
     ($ PeriodElement
        {:className "period" & (dissoc props :className)})))


(defnc PeriodInput
  [{:keys [disabled
           placeholder
           className
           open
           opened
           format]
    [from to :as value] :value
    :or {format :medium-datetime
         wrapper "div"}}]
  (let [translate (th/use-translate)
        input (hooks/use-ref nil)]
    ($ ui/wrapper
     {:onClick (fn []
                 (when @input (.focus @input))
                 (open))
      :className (str className
                      (when opened (str " opened"))
                      (when disabled " disabled"))}
     ($ autosize-input
        {:ref input
         :className "input"
         :readOnly true
         :value (if (or (nil? value) (every? nil? value))
                  nil
                  (str
                    (if from (translate from format) " ")
                    " - "
                    (if to (translate to format) " ")))
         :spellCheck false
         :auto-complete "off"
         :disabled disabled
         :placeholder placeholder}))))


(defnc PeriodElementProvider
  [{:keys [disabled
           read-only
           onChange]
    [upstream-start upstream-end] :value
    :or {upstream-start nil upstream-end nil}
    :as props}]
  (let [[[{start-value :selected :as start} {end-value :selected :as end}] set-state!]
        (hooks/use-state [(if (some? upstream-start)
                            (assoc (-> upstream-start vura/time->value vura/day-time-context) :selected upstream-start)
                            {:selected upstream-start})
                          (if (some? upstream-end)
                            (assoc (-> upstream-end vura/time->value vura/day-time-context) :selected upstream-end)
                            {:selected upstream-end})])
        ;;
        set-state! (hooks/use-memo
                    [disabled read-only]
                    (if (or disabled read-only)
                      (fn [& _])
                      set-state!))
        selected? (hooks/use-memo
                   [start-value end-value]
                   (fn [data]
                     (letfn [(->value [{:keys [year month day-in-month]}]
                               (vura/utc-date-value year month day-in-month))]
                       (let [start (when start-value (-> start-value vura/date->value vura/midnight))
                             current (->value data)
                             end (when end-value (-> end-value vura/date->value vura/midnight))]
                         (cond
                           (nil? start) (<= current end)
                           (nil? end) (>= current start)
                           :else
                           (or
                            (= start current)
                            (= end current)
                            (<= start current end)))))))
        [set-start! set-end!] (hooks/use-memo
                               :once
                               [(fn [value]
                                  (set-state! assoc 0 value))
                                (fn [value]
                                  (set-state! assoc 1 value))])
        start-events (use-timestamp-events start set-start!)
        end-events (use-timestamp-events end set-end!)]
    (hooks/use-effect
     [start-value end-value]
     (onChange [start-value end-value]))
    (hooks/use-effect
     [upstream-start upstream-end]
     (when (= [nil nil] [upstream-start upstream-end])
       (set-state! [nil nil])))
    ;;
    (provider
     {:context *calendar-selected*
      :value selected?}
     (provider
      {:context *calendar-events*
       :value {:start start-events
               :end end-events}}
      (provider
       {:context *calendar-state*
        :value [start end]}
       (provider
        {:context *calendar-disabled*
         :value false}
        (c/children props)))))))


(defnc PeriodDropdown
  [{:keys [disabled
           value
           read-only
           onChange]
    :as props}]
  (let [state (use-calendar-state)
        area (hooks/use-ref nil)
        [opened set-opened!] (hooks/use-state false)
        popup (hooks/use-ref nil)
        cache (hooks/use-ref value)]
    ;;
    (hooks/use-effect
     [state]
     (if state
       (reset! cache state)
       (reset! cache nil)))
    ;;
    (popup/use-outside-action
     opened area popup
     #(do
        (set-opened! false)
        (when (and
               (ifn? onChange)
               (or (not= (:selected (nth @cache 0)) (nth value 0))
                   (not= (:selected (nth @cache 1)) (nth value 1))))
          (onChange [(:selected (nth @cache 0)) (:selected (nth @cache 1))]))))
    ($ popup/Area
       {:ref area}
       ($ ui/field {:open (fn [] (set-opened! not)) :opened opened & props})
       ; (c/children props)
       (when (and (not read-only) (not disabled) opened)
         ($ ui/popup {:ref popup :value state})))))
