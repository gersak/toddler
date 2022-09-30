(ns toddler.elements.date
  (:require
    [vura.core :as vura]
    [helix.core :refer [$ defnc defhook create-context provider]]
    [helix.dom :as d]))


;; CALENDAR
(def ^:dynamic ^js *calendar-events* (create-context))
(def ^:dynamic ^js *calendar-selected* (create-context))
(def ^:dynamic ^js *calendar-disabled* (create-context (constantly false)))
(def ^:dynamic ^js *calendar-control* (create-context))
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


(defnc CalendarWeek [{:keys [days className]}]
  (let [days (group-by :day days)
        calendar-day (hooks/use-context *calendar-day*)]
    (d/div
     {:class className}
     (d/div
      {:class "week-days"}
      ($ calendar-day {:key 1 :day 1 & (get-in days [1 0] {})})
      ($ calendar-day {:key 2 :day 2 & (get-in days [2 0] {})})
      ($ calendar-day {:key 3 :day 3 & (get-in days [3 0] {})})
      ($ calendar-day {:key 4 :day 4 & (get-in days [4 0] {})})
      ($ calendar-day {:key 5 :day 5 & (get-in days [5 0] {})})
      ($ calendar-day {:key 6 :day 6 & (get-in days [6 0] {})})
      ($ calendar-day {:key 7 :day 7 & (get-in days [7 0] {})})))))


(def ^:dynamic *calendar-week* (create-context))


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


(def ^:dynamic *calendar-month-header* (create-context))


(defnc CalendarMonth
  [{:keys [className days]}]
  (let [weeks (sort-by key (group-by :week days))
        month-header (hooks/use-context *calendar-month-header*)
        calendar-week (hooks/use-context *calendar-week*)]
    (d/div
      {:class className}
      ($ month-header {:days (range 1 8)})
      (map
        #($ calendar-week {:key (key %) :week (key %) :days (val %)})
        weeks))))


(defnc CalendarMonthDropdown
  [{:keys [value placeholder className]
    :or {placeholder "-"}
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
    ($ DropdownElement
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
    ;;
    ($ DropdownElement
       {:placeholder placeholder
        :className className
        & props'})))


(defnc TimestampInput
  [{:keys [value
           placeholder
           className
           opened
           format]
    :or {format :datetime}}]
  (let [{:keys [open]} (hooks/use-context *calendar-control*)
        disabled (hooks/use-context *calendar-disabled*)
        translate (use-translate)]
    (d/div
     {:onClick open
      :className (str className (when opened (str " opened")))}
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
        {:spellCheck false
         :auto-complete "off"
         & (dissoc props' :constraints :delimiters :mask)}))))



(defnc TimestampClear
  [{:keys [className] :as props}]
  (let [{:keys [on-clear]} (use-calendar-events)
        disabled (hooks/use-context *calendar-disabled*)]
    (d/div
     {:className className
      :onClick (when-not disabled on-clear)}
     (c/children props))))


(defnc TimestampCalendar
  [{:keys [year month day-in-month className
           render/previous render/next]}]
  (let [now (-> (vura/date) vura/time->value)
        year (or year (vura/year? now))
        month (or month (vura/month? now))
        day-in-month (or day-in-month (vura/day-in-month? now))
        days (hooks/use-memo
              [year month]
              (vura/calendar-frame
               (vura/date->value (vura/date year month))
               :month))
        {:keys [on-next-month on-prev-month]} (use-calendar-events)]
    (d/div
     {:className className}
     (d/div
      {:className "header-wrapper"}
      (d/div
       {:className "header"}
       (d/div
        {:className "years"}
        ($ previous
           {:onClick on-prev-month
            :className "button"})
        ($ calendar-year-dropdown {:value year}))
       (d/div
        {:className "months"}
        ($ calendar-month-dropdown {:value month})
        ($ next
           {:onClick on-next-month
            :className "button"}))))
     (d/div
      {:className "content-wrapper"}
      (d/div
       {:className "content"}
       ($ calendar-month {:value day-in-month :days days}))))))



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
    rcalendar :render/calendar
    value :value
    :or {rcalendar timestamp-calendar}}]
  (let [[{:keys [selected] :as state} set-state!] (hooks/use-state nil)
        set-state! (hooks/use-memo
                    [disabled read-only]
                    (if (or disabled read-only)
                      (fn [& _])
                      set-state!))
        ;;
        disabled false
        read-only false
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
          ($ rcalendar {& state})))))))


(defnc TimestampPopup
  [{:keys [year month day-in-month hour minute className]
    rcalendar :render/calendar
    rtime :render/time
    rclear :render/clear
    wrapper :render/wrapper}
   popup]
  {:wrap [(react/forwardRef)]}
  ($ popup/Element
    {:ref popup
     :className className 
     :wrapper wrapper
     :preference popup/cross-preference}
    ($ rcalendar
       {:year year
        :month month
        :day-in-month day-in-month})
    (when (or rtime rclear)
      (d/div
        {:style
         {:display "flex"
          :flex-grow "1"
          :justify-content "center"}}
        (when rtime
          ($ rtime
             {:hour hour
              :minute minute}))
        (when rclear ($ rclear))))))


(defnc TimestampDropdownElement
  [{:keys [value
           onChange
           disabled
           read-only]
    rfield :render/field
    rpopup :render/popup
    :or {rfield TimestampInput
         rpopup TimestampPopup}
    :as props}]
  (let [[{:keys [year month day-in-month]
          :as state} set-state!]
        (hooks/use-state
          (some->
            value
            vura/time->value
            vura/day-time-context))
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
                    [year day-in-month month]
                    (fn [props]
                      (=
                        (select-keys
                          props
                          [:day-in-month :year :month])
                        {:year year
                         :day-in-month day-in-month
                         :month month})))
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
      {:context *calendar-control*
       :value {:open #(set-opened! true)
               :close #(do
                         (set-opened! false)
                         (when (and (ifn? onChange) (not= @cache value))
                           (onChange @cache)))}}
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
                 ($ rfield {:opened opened & props})
                 (when (and (not read-only) (not disabled) opened)
                   ($ rpopup {:ref popup & state}))))))))))
