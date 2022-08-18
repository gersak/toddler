(ns toddler.calendar-stories
  (:require
   [vura.core :as vura]
   [helix.core :refer [$ <>]]
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

(defn ^:export CalendarWeek
  []
  ($ interactions/calendar-week
     {:days
      (->
        (vura/date)
        vura/date->value
        (vura/calendar-frame :week))}))

(defn ^:export TimeTest
  []
  ($ interactions/TimestampTime
     {:hour (-> (vura/date) vura/time->value vura/hour?)
      :minute (-> (vura/date) vura/time->value vura/minute?)}))

(defn ^:export SliderTest
  []
  (let [[state set-state] (hooks/use-state 0)]
    ($ interactions/slider {:min "0"
                            :max "500"
                            :value (str state)
                            :onChange (fn [e] (set-state (.-value (.-target e))))})))

(defn ^:export CellTest
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

(defn ^:export CalendarYearDropdownTest
  []
  ($ popup/Container
     ($ interactions/calendar-month-dropdown
        {:value (-> (vura/date) vura/day-time-context)
         :placeholder "Click me"})))

(defn ^:export SearchTest
  []
  (let [[state set-state!] (hooks/use-state "")]
    ($ interactions/search
       {:value state
        :on-change (fn [e] (set-state! (.-value (.-target e))))})))

(defn ^:export CheckBoxTest
  []
  (let [[state set-state!] (hooks/use-state false)] ($ interactions/checkbox
                                                       {:active state
                                                        :onClick (fn [] (set-state! (not state)))})))

(defn ^:export DropDownTest
  []
  ($ interactions/DropdownArea
     ($ interactions/DropdownElement {:value "dsfdgsg"})
     ($ interactions/DropdownElement {:value "dfds"})))

(defn ^:export ValuteTest
  []
  (let [[state set-state!] (hooks/use-state 0)]
    ($ interactions/CurrencyField {:currency "EUR"
                                   :amount state
                                   :placeholder "evro"
                                   :onChange (fn [e] (set-state! (.-value (.-target e))))})))

(defn ^:export NumberInput
  []
  (let [[state set-state!] (hooks/use-state "")]
    ($ interactions/InputField
       {:value state
        :onChange (fn [e] (set-state! (.. e -target -value)))})))

(defn ^:export TimeStampCalendarTest
  []
  ($ popup/Container
     (d/div
       {:style {:margin "auto",
                :width "20%"}}
       (let []
         ($ interactions/timestamp-calendar
            {:onChange (fn [x] (.log js/console "clicked day"))})))))

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