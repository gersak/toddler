(ns toddler.calendar-stories
  (:require
   [vura.core :as vura]
   [helix.core :refer [$ <> provider]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   toddler.theme
   [toddler.interactions :as interactions]
   [toddler.elements.popup :as popup]
   ["react" :as react]))



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
             :month)}
     ($ interactions/calendar-year-dropdown
        {:value (vura/year? (vura/date))
         :placeholder "Click me"})))


(defn ^:export CalendarYearDropdownTest
  []
  ($ popup/Container
     ($ interactions/calendar-month-dropdown
        {:value (-> (vura/date) vura/day-time-context)
         :placeholder "Click me"})))

(defn ^:export CheckBox
  []
  (let [[state set-state!] (hooks/use-state false)] ($ interactions/checkbox
                                                       {:active state
                                                        :onClick (fn [] (set-state! (not state)))})))

(defn ^:export PopupCalendarWithTimer
  []
  ($ popup/Container
     ($ interactions/TimestampElement
        {:placeholder "Click here"})))

(defn ^:export FullChangeableCalendar
  []
  {:wrap [(react/forwardRef)]}
  (let [[{:keys [year month day-in-month]
          :as state} set-state!]
        (hooks/use-state
         (some->
          (vura/date)
          vura/time->value
          vura/day-time-context))
        disabled false
        read-only false
        set-state! (hooks/use-memo
                    [disabled read-only]
                    (if (or disabled read-only)
                      (fn [& _])
                      set-state!))
        events (interactions/use-timestamp-events set-state! state)
        cache (hooks/use-ref state)
        selected? (hooks/use-memo
                   [year day-in-month month]
                   (fn [props]
                     (=
                      (select-keys
                       props
                       [:day-in-month :year :month])
                      {:year year
                       :day-in-month day-in-month
                       :month month})))]
    (hooks/use-effect
     [state]
     (if state
       (reset! cache (-> state vura/context->value vura/value->date))
       (reset! cache nil)))
    ($ popup/Container
       (provider
        {:context interactions/*calendar-selected*
         :value selected?}
        (provider
         {:context interactions/*calendar-events*
          :value events}
         (provider
          {:context interactions/*calendar-disabled*
           :value disabled}
          ($ interactions/timestamp-calendar
             {& state})))))))

(defn ^:export AvatarImage
  []
  (let [[state set-state] (hooks/use-state 100)]
    (<>
     ($ interactions/slider
        {:width "300px"
         :min "10"
         :max "500"
         :value (str state)
         :onChange (fn [e] (set-state (.-value (.-target e))))})
     (d/br)
     ($ interactions/avatar
        {:size (int state)
         :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/1920px-Image_created_with_a_mobile_phone.png"}))))

#_(defn ^:export SearchTest
    []
    (let [[state set-state!] (hooks/use-state "")]
      ($ interactions/search
         {:value state
          :on-change (fn [e] (set-state! (.-value (.-target e))))})))

#_(defn ^:export DropDownTest
    []
    ($ interactions/DropdownArea
       ($ interactions/DropdownElement {:value "dsfdgsg"})
       ($ interactions/DropdownElement {:value "dfds"})))

#_(defn ^:export ValuteTest
    []
    (let [[state set-state!] (hooks/use-state 0)]
      ($ interactions/CurrencyField {:currency "EUR"
                                     :amount state
                                     :placeholder "evro"
                                     :onChange (fn [e] (set-state! (.-value (.-target e))))})))

#_(defn ^:export NumberInput
    []
    (let [[state set-state!] (hooks/use-state "")]
      ($ interactions/InputField
         {:value state
          :onChange (fn [e] (set-state! (.. e -target -value)))})))
#_(defn ^:export CalendarWeek
    []
    ($ interactions/calendar-week
       {:days
        (->
         (vura/date)
         vura/date->value
         (vura/calendar-frame :week))}))

#_(defn ^:export TimeTest
    []
    ($ interactions/TimestampTime
       {:hour (-> (vura/date) vura/time->value vura/hour?)
        :minute (-> (vura/date) vura/time->value vura/minute?)}))

#_(defn ^:export SliderTest
    []
    (let [[state set-state] (hooks/use-state 0)]
      ($ interactions/slider {:min "0"
                              :max "500"
                              :value (str state)
                              :onChange (fn [e] (set-state (.-value (.-target e))))})))

#_(defn ^:export CellTest
    []
    ($ interactions/integer-cell {:cell/data 25}))

#_(defn ^:export TimeStampInputTest
    []
    (let [[state set-state] (hooks/use-state (vura/date))]
      ($ interactions/TimestampFieldInput
         {:placeholder "" #_(vura/date)
          :value state
          :format "yyyy-mm-ddThh:mm:ss.SSS"
          :onChange (fn [e] (set-state (.. e target value)))})))
