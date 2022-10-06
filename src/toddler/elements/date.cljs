(ns toddler.elements.date
  (:require
    ["react" :as react]
    [goog.string :as gstr]
    [goog.string.format]
    [vura.core :as vura]
    [helix.core :refer [$ defnc defhook create-context provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.styled-components :refer [defstyled]]
    [helix.children :as c]
    [toddler.ui :as ui]
    [toddler.hooks
     :refer [use-calendar
             use-translate]]
    [toddler.elements.mask :refer [use-mask]]
    [toddler.elements.dropdown :as dropdown]
    [toddler.elements.popup :as popup]
    [toddler.elements.input :refer [AutosizeInput]]
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


(defnc CalendarDay
  [{:keys [value
           day-in-month
           className] :as props}]
  (let [{on-select :on-day-change} (hooks/use-context *calendar-events*)
        is-disabled (hooks/use-context *calendar-disabled*)
        is-selected (hooks/use-context *calendar-selected*)
        disabled (when (ifn? is-disabled) (is-disabled props))
        selected (when (ifn? is-selected) (is-selected props))
        is-today (= (select-keys
                     (-> (vura/date) vura/day-time-context) [:day-in-month :year :month])
                    (cond (some? value)
                          (select-keys
                           (-> value vura/day-time-context) [:day-in-month :year :month])))
        is-weekend (cond (some? value)
                         (if (vura/weekend? value)
                           true false))
        #_is-holiday #_(cond (some? value)
                             (if (-> value vura/*holiday?*)
                               "red" ""))]
    (d/div
     {:class className}
     (d/div
      {:class (cond-> ["day"]
                selected (conj "selected")
                disabled (conj "disabled")
                is-today (conj "today")
                is-weekend (conj "weekend")
                (nil? value) (conj "empty"))
       :onClick (fn []
                  (when-not disabled
                    (when (fn? on-select)
                      (on-select day-in-month))))}
      (d/div (or day-in-month " "))))))


(def ^:dynamic ^js *calendar-day* (create-context))


(defnc CalendarWeek
  [{:keys [days className]}]
  (let [days (group-by :day days)]
    (d/div
      {:class className}
      (d/div
        {:class "week-days"}
        ($ ui/calendar-day {:key 1 :day 1 & (get-in days [1 0] {})})
        ($ ui/calendar-day {:key 2 :day 2 & (get-in days [2 0] {})})
        ($ ui/calendar-day {:key 3 :day 3 & (get-in days [3 0] {})})
        ($ ui/calendar-day {:key 4 :day 4 & (get-in days [4 0] {})})
        ($ ui/calendar-day {:key 5 :day 5 & (get-in days [5 0] {})})
        ($ ui/calendar-day {:key 6 :day 6 & (get-in days [6 0] {})})
        ($ ui/calendar-day {:key 7 :day 7 & (get-in days [7 0] {})})))))


(defnc CalendarMonthHeader
  [{:keys [className days]}]
  (let [week-days (use-calendar :weekdays/short)
        day-names (zipmap
                    [7 1 2 3 4 5 6]
                    week-days)]
    (d/div
      {:class className}
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


(defnc CalendarMonth
  [{:keys [className days]}]
  (let [weeks (sort-by key (group-by :week days))]
    (d/div
      {:class className}
      ($ ui/header {:days (range 1 8)})
      (map
        #($ ui/calendar-week {:key (key %) :week (key %) :days (val %)})
        weeks))))


(defnc CalendarMonthDropdown
  [{:keys [value placeholder className]
    :or {placeholder "-"
         rinput autosize-input}
    :as props}]
  (let [value (or value (vura/month? (vura/date)))
        {on-month-change :on-month-change} (use-calendar-events)
        months (range 1 13)
        month-names (use-calendar :months)
        search-fn (zipmap months month-names)
        props' (assoc props
                      :onChange on-month-change
                      :search-fn search-fn
                      :options months
                      :position-preference popup/central-preference
                      :value value)]
    ($ dropdown/Element
       {:placeholder placeholder
        :className className
        & props'})))


(defnc CalendarYearDropdown
  [{:keys [value placeholder className]
    :or {placeholder "-"}
    :as props}]
  (let [value (or value (vura/year? (vura/date)))
        {on-year-change :on-year-change} (use-calendar-events)
        props' (assoc props
                      :value value
                      :onChange on-year-change
                      :position-preference popup/central-preference
                      :options
                      (let [year (vura/year? (vura/date))]
                        (range (- year 5) (+ year 5))))]
    (println "FOR REAL")
    ;;
    ($ dropdown/Element
       {:placeholder placeholder
        :className className
        & props'})))


(defnc TimestampInput
  [{:keys [value
           placeholder
           className
           open
           opened
           format]
    :or {format :datetime}}]
  (let [disabled (hooks/use-context *calendar-disabled*)
        translate (use-translate)]
    ($ ui/wrapper
     {:onClick open
      :className (str className 
                      (when opened " opened")
                      (when disabled " disabled"))}
     ($ autosize-input
        {:className "input"
         :readOnly true
         :value (when (some? value) (translate value format))
         :spellCheck false
         :auto-complete "off"
         :disabled disabled
         :placeholder placeholder}))))


(defnc TimestampTime
  [{:keys [className hour minute]}]
  (let [hour (or hour 0)
        minute (or minute 0)
        {:keys [on-time-change]} (use-calendar-events)
        disabled (hooks/use-context *calendar-disabled*)
        props' (use-mask
                 {:value (gstr/format "%02d:%02d" hour minute)
                  :className className
                  :disabled disabled
                  :mask (gstr/format "%02d:%02d" 0 0)
                  :delimiters #{\:}
                  :constraints [#"([0-1][0-9])|(2[0-3])" #"[0-5][0-9]"]
                  :onChange (fn [time-]
                              (let [[h m] (map js/parseInt (clojure.string/split time- #":"))]
                                (when (ifn? on-time-change)
                                  (on-time-change {:hour h :minute m}))))})]
    (d/div
      {:className className}
      (d/input
        {:className "time"
         :spellCheck false
         :auto-complete "off"
         & (dissoc props' :className :constraints :delimiters :mask)}))))



(defnc TimestampClear
  [{:keys [className] :as props}]
  (let [{:keys [on-clear]} (use-calendar-events)
        disabled (hooks/use-context *calendar-disabled*)]
    (d/div
     {:className className
      :onClick (when-not disabled on-clear)}
     ($ icon/clear
        {:onClick (when-not disabled on-clear)}))))


(defnc TimestampCalendar
  [{:keys [year month day-in-month className]}]
  (let [now (-> (vura/date) vura/time->value)
        year (or year (vura/year? now))
        month (or month (vura/month? now))
        day-in-month (or day-in-month (vura/day-in-month? now))
        days (hooks/use-memo
               [year month]
               (vura/calendar-frame
                 (vura/date->value (vura/date year month))
                 :month))
        {:keys [on-next-month on-prev-month]} (use-calendar-events)
        components (hooks/use-context ui/__components__)]
    (d/div
      {:className className}
      (d/div
        {:className "header-wrapper"}
        (d/div
          {:className "header"}
          (d/div
            {:className "years"}
            ($ icon/previous
               {:onClick on-prev-month
                :className "button"})
            ($ ui/calendar-year-dropdown {:value year}))
          (d/div
            {:className "months"}
            ($ ui/calendar-month-dropdown {:value month})
            ($ icon/next
               {:onClick on-next-month
                :className "button"}))))
      (d/div
        {:className "content-wrapper"}
        (d/div
          {:className "content"}
          ($ ui/calendar-month {:value day-in-month :days days}))))))



(defhook use-timestamp-events
  [set-timestamp! {:keys [day-in-month year selected] :as timestamp}]
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
                        vura/context->value
                        vura/value->time)
          :day-in-month day-in-month)))
      ;;
      :on-year-change #(set-timestamp! (assoc timestamp :year %))
      :on-month-change (fn [month]
                         (let [v (vura/utc-date-value year month)
                               {last-day :days-in-month} (vura/day-context v)]
                           (set-timestamp!
                            (assoc timestamp
                                   :month month
                                   :day-in-month (min day-in-month last-day)))))
      :on-time-change #(set-timestamp! (merge timestamp %))})))


(defnc TimestampCalendarElement
  [{:keys [disabled
           read-only
           onChange]
    :or {disabled false
         read-only false}
    value :value}]
  (let [[{:keys [selected] :as state} set-state!] (hooks/use-state nil)
        set-state! (hooks/use-memo
                    [disabled read-only]
                    (if (or disabled read-only)
                      (fn [& _])
                      set-state!))
        events (use-timestamp-events set-state! state)
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
                         :month month}))))]
    (hooks/use-effect
     [value]
      ;; When value has changed... Compare it with current local state
      ;; If it doesn't match, update local state
     (when (not= value (when (:value state) selected))
       (-> (if value value (vura/date))
           vura/time->value
           vura/day-time-context
           (assoc :selected value)
           set-state!)))
    ;; When local state changes, notify upstream listener
    ;; that new value has been selected
    (hooks/use-effect
     [state]
     (when (and (fn? onChange)
                (not= selected value))
       (onChange
        (when state
          (-> state vura/context->value vura/value->time)))))
    ($ popup/Container
       (provider
        {:context *calendar-selected*
         :value selected?}
        (provider
         {:context *calendar-events*
          :value events}
         (provider
          {:context *calendar-disabled*
           :value disabled}
          ($ ui/calendar {& state})))))))


(defnc TimestampPopup
  [{:keys [year month day-in-month hour minute className]} popup]
  {:wrap [(react/forwardRef)]}
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
      ($ ui/clear))))


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
        events (use-timestamp-events set-state! state)
        cache (hooks/use-ref state)]
    ;; TODO - timestamp should be a little more complex than this
    ;; It shouldn't have single popup props since those props are used
    ;; to navigate calendar. Calendar itself should have props that are
    ;; responsible for calendar navigation, but that affects use-calendar-events
    ;; hook. Point is that two states are required. One for calendar and one for
    ;; current value context (currently implemented)
    ;; Good enough for now lets move on!
    (hooks/use-effect
      [value]
      (if (some? value)
        (let [ov (-> state
                     vura/context->value
                     vura/value->time)]
          (when-not (= value ov)
            (-> value
                vura/time->value
                vura/day-time-context
                set-state!)))
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
  (let [translate (use-translate)
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
        start-events (use-timestamp-events set-start! start)
        end-events (use-timestamp-events set-end! end)]
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
