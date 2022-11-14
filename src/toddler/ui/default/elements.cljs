(ns toddler.ui.default.elements
  (:require
    ["react" :as react]
    [goog.string :as gstr]
    [vura.core :as vura]
    [clojure.string :as str]
    [shadow.css :refer [css]]
    [helix.core
     :refer [$ defnc provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.children :as c]
    [toddler.input
     :refer [AutosizeInput
             IdleInput]]
    [toddler.ui :refer [forward-ref]]
    [toddler.avatar :as a]
    [toddler.dropdown :as dropdown]
    [toddler.multiselect :as multiselect]
    [toddler.mask :refer [use-mask]]
    [toddler.date :as date]
    [toddler.scroll :as scroll]
    [toddler.popup :as popup]
    [toddler.ui.provider :refer [ExtendUI UI]]
    ["toddler-icons$default" :as icon]))


(defnc simplebar
  [{:keys [className shadow hidden] :as props
    :or {shadow #{}}}
   _ref]
  {:wrap (forward-ref)}
  (let [$default (css {:transition "box-shadow 0.3s ease-in-out"})
        $shadow-top (css {:box-shadow "inset 0px 11px 8px -10px #CCC"})
        $shadow-bottom (css {:box-shadow "inset 0px -11px 8px -10px #CCC"}) 
        $hidden (css ["& .simplebar-track" {:display "none"}])
        className (str/join
                    " "
                    (cond-> [className $default]
                      (shadow :top) (conj $shadow-top) 
                      (shadow :bottom) (conj $shadow-bottom)
                      hidden (conj $hidden)))]
    ($ scroll/_SimpleBar
       {:className className
        :ref _ref
        & (-> props
              (dissoc :shadow :className :class)
              (:style scroll/transform-style))}
       (c/children props))))


(defnc autosize-input
  [props]
  ($ AutosizeInput
    {:className (css
                  :border-0
                  :outline-none)
     & props}))


(defnc idle-input
  [props]
  ($ IdleInput
    {:className (css {:outline "none"
                      :border "none"})
     & props}))


(defnc buttons
  [props]
  (let [$wrapper (css
                   :display "flex"
                   :button {:m-0
                            :rounded-none}
                   :button:first-of-type {:border-top-left-radius "4px" :border-bottom-left-radius "4px"}
                   :button:last-of-type {:border-top-right-radius "4px" :border-bottom-right-radius "4px"})]
    (d/div
      {:className $wrapper}
      (c/children props))))


(defn button-color
  [{:keys [context disabled]}]
  (let [context (if disabled :disabled context)
        $positive
        (css :text-green-200
             :bg-green-600
             [:hover :text-white :bg-green-500])
        ;;
        $negative
        (css
          :text-red-600
          :bg-red-200
          [:hover :bg-red-400 :text-red-900])
        $fun
        (css 
          :bg-cyan-500
          :text-cyan-100
          [:hover :bg-cyan-400])
        ;;
        $fresh
        (css
          :text-yellow-900
          :bg-yellow-300
          [:hover :text-black :bg-yellow-400]) 
        ;;
        $stale
        (css
          {:color :gray
           :background-color :gray/light}
          [:hover {:background-color "#d9d9d9"}])
        ;;
        $disabled
        (css 
          {:color "white"
           :background-color "#bbbbbb"
           :cursor "initial"
           :pointer-events "none"
           :user-select "none"})
        ;;
        $default
        (css :text-teal-900
             :bg-cyan-400
             [:hover :bg-cyan-300])]
    (case context
      :positive $positive
      :negative $negative
      :fun $fun
      :fresh $fresh
      :stale $stale
      :disabled $disabled
      $default)))


(defnc button
  [{:keys [disabled] :as props}]
  (let [$color (button-color props) 
        $layout (css 
                  :flex
                  :border-2
                  :border-transparent
                  :rounded-sm
                  ; :px-18
                  ; :py-4
                  {:justify-content "center"
                   :padding "8px 18px"
                   :align-items "center"
                   :max-height "30px"
                   :min-width "80px"
                   :font-size "1em"
                   :line-height "1.33"
                   :text-align "center"
                   :vertical-align "center"
                   :margin "3px 2px"
                   :transition "box-shadow .3s ease-in,background .3s ease-in"
                   :cursor "pointer"}
                  ["&:hover" {:transition "background .3s ease-in"}]
                  ["&:focus" {:outline "none"}]
                  ["&:active" {:transform "translate(0px,2px)" :box-shadow "none"}])
        $shadow (css
                  ["&:hover" {:box-shadow "0px 2px 4px 0px #aeaeae"}])
        $disabled (css :pointer-events "none")]
    (d/button
      {:class (cond-> [$layout
                       $color
                       $shadow]
                disabled (conj $disabled))}
      (c/children props))))


(defnc checkbox
  [{:keys [value disabled] :as props}]
  (let [$layout (css
                  {:cursor "pointer"
                   :transition "color .2s ease-in"
                   :width "1.5em" 
                   :height "1.5em"
                   :border-radius "4px"
                   :border-color "transparent"
                   :padding "0px"
                   :display "flex"
                   :justify-content "center"
                   :outline "none"
                   :align-items "center"}
                  ["& path" {:cursor "pointer"}]
                  ["&:active" {:border-color "transparent"}])
        $active (css
                  :text-white
                  :bg-green-500)
        $inactive (css
                    :text-white
                    :bg-gray-400)
        $disabled (css :pointer-events "none")]
    (d/button
      {:class [$layout
               (if value $active $inactive)
               (when disabled $disabled)]
       & (dissoc props :value)}
      ($ (case value
           nil icon/checkboxDefault
           icon/checkbox)))))



(defnc row
  [{:keys [label position] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :text-gray-800
                  {:display "flex"
                   :flex-direction "row"
                   :align-items "center"
                   :flex-grow "1"}
                  ["& .label"
                   {:margin "4px 4px 4px 4px"
                    :padding-bottom "2px"
                    :text-transform "uppercase"
                    :font-size "1em"
                    :color :text-gray-800}])
        $start (css {:justify-content "flex-start"})
        $center (css {:justify-content "center"})
        $end (css {:justify-content "flex-end"})
        $explode (css {:justify-content "space-between"})
        $position (case position
                    :center $center
                    :end $end
                    :explode $explode
                    $start)]
    (d/div
      {:ref _ref
       :class [$layout
               $position]
       :position position}
      (when label
        (d/div
          {:className "label"}
          (d/label label)))
      (c/children props))))


(defnc column
  [{:keys [label position] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :text-gray-800
                  {:display "flex"
                   :flex-direction "column"
                   :align-items "center"
                   :flex-grow "1"}
                  ["& .label"
                   {:margin "4px 4px 4px 4px"
                    :padding-bottom "2px"
                    :text-transform "uppercase"
                    :font-size "1em"
                    :color :text-gray-800}])
        $start (css {:justify-content "flex-start"})
        $center (css {:justify-content "center"})
        $end (css {:justify-content "flex-end"})
        $explode (css {:justify-content "space-between"})
        $position (case position
                    :center $center
                    :end $end
                    :explode $explode
                    $start)]
    (d/div
      {:ref _ref
       :class [$layout
               $position]
       :position position}
      (when label
        (d/div
          {:className "label"}
          (d/label label)))
      (c/children props))))


(defnc dropdown-wrapper
  [{:keys [style] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :flex
                  :flex-col
                  :m-0
                  :p-2
                  :bg-white
                  :shadow-xl
                  :rounded-sm
                  :border-2
                  :border
                  :border-gray-100
                  {:box-shadow "0 11px 25px -5px rgb(0 0 0 / 9%), 0 4px 20px 0px rgb(0 0 0 / 14%)"}
                  ; {:box-shadow "0px 3px 10px -3px black"}
                  ["& .simplebar-scrollbar:before"
                   :bg-gray-100
                   :pointer-events-none
                   {:max-height "400px"}])]
    (d/div
      {:ref _ref
       :class [$layout]
       :style style}
      (c/children props))))


(defnc dropdown-option
  [props]
  (let [$layout (css
                  :flex
                  :justify-start
                  :items-center
                  :cursor-pointer
                  :text-gray-500
                  :rounded-sm
                  :bg-white
                  {:transition "color .2s ease-in,background-color .2s ease-in"
                   :padding "4px 6px 4px 4px"}
                  [:hover :text-gray-600 :bg-cyan-100]
                  ["&:last-child" {:border-bottom "none"}])]
    (d/div
      {:class [$layout]
       & props}
      (c/children props))))


(defnc DropdownPopup
  [props]
  ($ ExtendUI
    {:components {:wrapper dropdown-wrapper
                  :option dropdown-option}}
    ($ dropdown/Popup
       {& props} 
       (c/children props))))


(defnc calendar-month-dropdown
  [{:keys [value] :as props}]
  (let [$wrapper (css
                   {:margin "5px 0"
                    :cursor "pointer"}
                   ["& input" :cursor-pointer :text-rose-700])
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        months (range 1 13)
        search-fn (date/use-calendar-months) 
        ;;
        {:keys [area toggle!] :as dropdown}
        (dropdown/use-dropdown
          (->
            props
            (assoc :area-position area-position
                   :value (or value (vura/month? (vura/date)))
                   :search-fn search-fn 
                   :options months)
            (dissoc :className)))]
    (provider
      {:context dropdown/*dropdown*
       :value dropdown}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
        ($ popup/Area
           {:ref area
            :onClick (fn [] (toggle!))
            :className $wrapper}
           ($ dropdown/Input)
           ($ dropdown/Popup
              {:className "dropdown-popup"
               :render/option dropdown-option
               :render/wrapper dropdown-wrapper}))))))

(defnc calendar-year-dropdown
  [{:keys [value] :as props}]
  (let [$wrapper (css
                   {:margin "5px 0"
                    :cursor "pointer"}
                   ["& input" :cursor-pointer :text-rose-700])
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        years (date/use-calendar-years)
        ;;
        {:keys [area toggle!] :as dropdown}
        (dropdown/use-dropdown
          (->
            props
            (assoc :area-position area-position
                   :value (or value (vura/year? (vura/date)))
                   :options years)
            (dissoc :className)))]
    (provider
      {:context dropdown/*dropdown*
       :value dropdown}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
        ($ popup/Area
           {:ref area
            :onClick (fn [] (toggle!))
            :className $wrapper}
           ($ dropdown/Input
              {& props})
           ($ dropdown/Popup
              {:className "dropdown-popup"
               :render/option dropdown-option
               :render/wrapper dropdown-wrapper}))))))


(defnc multiselect-option 
  [props]
  (let [$style (css
                 :text-white
                 :bg-cyan-500
                 :rounded-sm
                 :px-2
                 :py-1
                 {:margin "3px"
                  :display "flex"
                  :flex-direction "row"
                  :justify-content "start"
                  :align-items "center"
                  :font-size "1em"}
                 ["& svg" {:margin "0 5px"
                           :padding-right "3px"}]
                 ["& .remove:hover" :text-black]
                 ["& .remove"
                  :text-cyan-700
                  :cursor-pointer
                  :flex
                  :items-center
                  :justify-center
                  {:transition "color .2s ease-in"}]
                 ["& .remove path" :cursor-pointer])]
    ($ multiselect/Option
       {:className $style & props})))


(defnc multiselect-wrapper
  [props]
  (d/div
    {:class (css
              {:display "flex"
               :justify-content "row"
               :align-items "center"
               :min-height "2.3em"})}
    (c/children props)))


(defnc MultiselectInput
  [props]
  ($ UI
    {:components {:input autosize-input
                  :wrapper multiselect-wrapper}}
    ($ dropdown/Input
       {& props}
       (c/children props))))


(defnc multiselect
  [props]
  (d/div
    {:class (css
              {:display "flex"
               :alignItems "center"})}
    ($ multiselect/Element
       {& props})))


(defnc calendar-day
  [props]
  (let [$style (css
                 :border
                 :border-transparent
                 :text-gray-500
                 ;;
                 ["& .day"
                  {:text-align "center"
                   :font-size "0.8em"
                   :user-select "none"
                   :padding "3px"
                   :width "25px"
                   :border-collapse "collapse"
                   :border "1px solid transparent"
                   :cursor "pointer"}]
                 ["& .day.today"
                  :border
                  :border-teal-600
                  :text-gray-800
                  :bg-cyan-300]
                 ;;
                 ["& .day.weekend" :text-rose-700]
                 ;;
                 ["& .day.empty" {:cursor "default"}]
                 ;;
                 ["& .day.disabled, & .day:hover.disabled"
                  :border
                  :border-solid
                  :border-transparent
                  :text-gray-500
                  :cursor-default]
                 ;;
                 ["& .day:hover:not(.empty), & .day.selected"
                  :text-white
                  :border
                  :rounded-sm
                  :border-cyan-800
                  :bg-cyan-500
                  :font-bold])]
    ($ date/CalendarDay
       {:className $style
        & (dissoc props :className :class)})))


(defnc calendar-month-header
  [{:keys [className days] :as props}]
  (let [$style (css
                 :flex
                 :flex-row
                 :cursor-default
                 {:cursor "default"}
                 ["& .day-wrapper .day" :text-gray-500]
                 ["& .day.weekend" :text-rose-700]
                 ["& .day-wrapper"
                  {:border-collapse "collapse"
                   :border "1px solid transparent"}]
                 ["& .day-wrapper .day"
                  {:text-align "center"
                   :font-weight "500"
                   :font-size "0.8em"
                   :border-collapse "collapse"
                   :user-select "none"
                   :padding "3px"
                   :width "25px"
                   :border "1px solid transparent"}])
        week-days (date/use-week-days)
        day-names (zipmap
                    [7 1 2 3 4 5 6]
                    week-days)]
    (d/div
      {:class [className $style]}
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


(defnc calendar-month
  [{:keys [days on-select disabled read-only]}]
  (let [$month (css
                 :flex
                 :flex-col
                 {:width "220px"})
        $week (css :flex :flex-row)
        weeks (sort-by key (group-by :week days))
        $day (css
               :border
               :border-transparent
               :text-gray-500
               ;;
               ["& .day"
                {:text-align "center"
                 :font-size "0.8em"
                 :user-select "none"
                 :padding "3px"
                 :width "25px"
                 :border-collapse "collapse"
                 :border "1px solid transparent"
                 :cursor "pointer"}]
               ["& .day.today"
                :border
                :border-teal-600
                :text-gray-800
                :bg-cyan-300]
               ;;
               ["& .day.weekend" :text-rose-700]
               ;;
               ["& .day.empty" {:cursor "default"}]
               ;;
               ["& .day.disabled, & .day:hover.disabled"
                :border
                :border-solid
                :border-transparent
                :text-gray-500
                :cursor-default]
               ;;
               ["& .day:hover:not(.empty), & .day.selected"
                :text-white
                :border
                :rounded-sm
                :border-cyan-800
                :bg-cyan-500
                :font-bold])
        today (-> (vura/date)
                  vura/time->value
                  vura/day-time-context)
        is-today (fn [day]
                   (date/same day today))]
    (d/div
      {:class $month}
      ($ calendar-month-header {:days (range 1 8)})
      (map
        (fn [[k days]]
          (let [week-days (group-by :day days)]
            (d/div
              {:key k
               :className $week}
              (map
                (fn [idx]
                  (let [{:keys [week day-in-month] :as day} (get-in week-days [idx 0] {})
                        today? (is-today day)
                        k (if (empty? day)
                            [idx :unknown]
                            [week day-in-month])]
                    ($ date/CalendarDay
                       {:key k
                        :day idx
                        :className $day
                        :today today? 
                        :onClick (fn []
                                   (when (and (not disabled) (not read-only))
                                     (when (fn? on-select)
                                       (on-select day)))) 
                        & day})))
                (range 1 8)))))
        weeks))))


(defnc timestamp-calendar
  [{:keys [value onChange] :as props}]
  (let [$style (css
                 {:display "flex"
                  :flex-direction "column"
                  :border-radius "3px"
                  :padding "7px"
                  :width "230px"
                  ; :height "190px"
                  }
                 ; (str popup/dropdown-container) {:overflow "hidden"}
                 ["& .header-wrapper" {:display "flex" :justify-content "center" :flex-grow "1"}]
                 ["& .header"
                  {:display "flex"
                   :justify-content "space-between"
                   :width "200px"
                   :height "38px"}]
                 ["& .header .years"
                  {:position "relative"
                   :display "flex"
                   :align-items "center"}]
                 ["& .header .months"
                  {:position "relative"
                   :display "flex"
                   :align-items "center"}]
                 ["& .content-wrapper"
                  {:display "flex"
                   :height "200px"
                   :justify-content "center"
                   :flex-grow "1"}])
        ;;
        {:keys [selected] :as props'}
        (hooks/use-memo
          [value onChange]
          (let [v (vura/time->value (or value (vura/date)))
                context (vura/day-time-context v)]
            (cond-> 
              (merge props context)
              (some? value) (assoc :selected v)
              (fn? onChange) (assoc
                               :onChange (fn [v]
                                           (when-not (= v value)
                                             (when v
                                               (onChange (vura/value->time v)))))))))
        ;;
        {:keys [days]
         {:keys [year month]} :state
         {:keys [on-prev-month
                 on-next-month
                 on-year-change
                 on-month-change
                 on-day-change]} :events} (date/use-calendar props' :month)
        upstream-is-selected (hooks/use-context date/*calendar-selected*)
        is-selected (hooks/use-memo
                      [selected]
                      (if-not selected (constantly false)
                        (let [selected-context (vura/day-context selected)]
                          (fn [day]
                            (date/same day selected-context)))))]
    (d/div
      {:className $style}
      (d/div
        {:className "header-wrapper"}
        (d/div
          {:className "header"}
          (d/div
            {:className "years"}
            ($ icon/previous
               {:onClick on-prev-month
                :className "button"})
            ($ calendar-year-dropdown
               {:value year
                :onChange on-year-change}))
          (d/div
            {:className "months"}
            ($ calendar-month-dropdown
               {:value month
                :onChange on-month-change})
            ($ icon/next
               {:onClick on-next-month
                :className "button"}))))
      (d/div
        {:className "content-wrapper"}
        (d/div
          {:className "content"}
          (provider
            {:context date/*calendar-selected*
             :value (or upstream-is-selected is-selected)}
            ($ calendar-month
               {:days days
                :on-select (fn [{:keys [day-in-month]}]
                             (on-day-change day-in-month))
                & props})))))))


(defnc timestamp-time
  [{:keys [value read-only disabled onChange] :as props}]
  (let [{:keys [hour minute] :as state} (hooks/use-memo
                                          [value]
                                          (if-not value {:hour 0 :minute 0}
                                            (->
                                              value
                                              vura/time->value
                                              vura/day-time-context)))
        props' (use-mask
                 {:value (gstr/format "%02d:%02d" hour minute)
                  :disabled disabled
                  :read-only read-only
                  :mask (gstr/format "%02d:%02d" 0 0)
                  :delimiters #{\:}
                  :constraints [#"([0-1][0-9])|(2[0-3])" #"[0-5][0-9]"]
                  :onChange (fn [time-]
                              (let [[h m] (map js/parseInt (str/split time- #":"))]
                                (when (ifn? onChange)
                                  (onChange
                                    (->
                                      state 
                                      (assoc :hour h :minute m)
                                      vura/context->value
                                      vura/value->time)))))})]
    (d/div
      {:className (css
                    {:display "flex"
                     :justify-content "center"
                     :align-items "center"

                     :font-size "1em"
                     :margin "3px 0 5px 0"
                     :justify-self "center"}
                    ["& input" {:max-width "40px"}]
                    ["& .time" {:outline "none"
                                :border "none"}])}
      (d/input
        {:className "time"
         :spellCheck false
         :auto-complete "off"
         & (dissoc props' :className :constraints :delimiters :mask)}))))


; (defnc timestamp-clear
;   [props]
;   ($ date/TimestampClear
;     {:className (css
;                   :flex
;                   :bg-gray-400
;                   :text-white
;                   :items-center
;                   :justify-center
;                   :cursor-pointer
;                   {:width "15px"
;                    :height "15px"
;                    :padding "4px"
;                    :justify-self "flex-end"
;                    :transition "background .3s ease-in-out"
;                    :border-radius "20px"}
;                   ["&:hover" :bg-red-500])
;      & props}))



(defnc period-calendar
  [{:keys [onChange] :as props
    [start end :as value] :value
    :or {value [nil nil]}}]
  (let [$wrapper (css :flex)
        [after before] (hooks/use-memo
                         [start end]
                         (let [start (some->
                                       start
                                       vura/date->value
                                       vura/midnight)
                               end (some->
                                     end
                                     vura/date->value
                                     vura/midnight
                                     (+ vura/day))]
                           [(fn [{:keys [value]}]
                              (when start
                                (if end
                                  (and (<= start value)
                                       (< value end))
                                  (<= start value))))
                            (fn [{:keys [value]}]
                              (when end
                                (if start
                                  (and (<= start value)
                                       (< value end))
                                  (< value end))))]))]
    (d/div
      {:className $wrapper}
      (provider
        {:context date/*calendar-selected*
         :value after}
        ($ timestamp-calendar
           {:key :start
            :value start
            :onChange (fn [v]
                        (when (fn? onChange)
                          (onChange (assoc value 0 v))))}))
      (provider
        {:context date/*calendar-selected*
         :value before}
        ($ timestamp-calendar
           {:key :end
            :value end
            :onChange (fn [v]
                        (when (fn? onChange)
                          (onChange (assoc value 1 v))))})))))

(defnc PeriodElement
  [props]
  ($ ExtendUI
    {:components
     {:calendar timestamp-calendar
      :calendar/time timestamp-time}}
    ($ date/PeriodElement {& props})))


(defnc period-popup
  [props _ref]
  {:wrap [react/forwardRef]}
  (let [$style (css ["& .period" :flex :flex-row])]
    ($ ExtendUI
       {:components
        {:calendar timestamp-calendar
         :calendar/time timestamp-time
         :wrapper dropdown-wrapper}}
       ($ date/PeriodPopup
          {:className $style
           & props :ref _ref}))))


(defnc avatar
  [{:keys [size className] :as props}]
  (let [size' (case size
                :small "1.5em"
                :medium "3.5em"
                :large "10em"
                size)]
    ($ a/Avatar
       {:className className
        :style {:border-radius 20
                :width size'
                :height size'}
        & props})))


(defnc tooltip
  [props]
  (c/children props))


(defnc card-action
  [{:keys [tooltip disabled onClick context] :as props}]
  ($ tooltip
    {:message tooltip
     :disabled (or (empty? tooltip) disabled)}
    (let [$wrapper (css
                     {:height "32px"
                      :width "32px"
                      :display "flex"
                      :justify-content "center"
                      :align-items "center"
                      :border-radius "32px"})
          $action (css
                    {:width "26px"
                     :height "26px"
                     :border-radius "26px"
                     :cursor "pointer"
                     :transition "color,background-color .2s ease-in-out"
                     :display "flex"
                     :color "#929292"
                     :justify-content "center"
                     :align-items "center"}
                    ["& svg" {:height "14px"
                              :width "14px"}])
          $default (css
                     [:hover :bg-cyan-500
                      {:color "#fff8f3"}])
          $negative (css
                      [:hover :bg-red-500
                       {:color "#fff8f3"}])]
      (d/div
        {:class [$wrapper
                 $action
                 (case context
                   :negative $negative
                   $default)]}
        (d/div
          {:className "action"
           :onClick onClick}
          (c/children props))))))


(defnc card-actions
  [props]
  (let [$layout (css
                  {:position "absolute"
                   :top "-16px"
                   :right "-16px"}
                  ["& .wrapper"
                    {:display "flex"
                     :flex-direction "row"
                     :justify-content "flex-end"
                     :align-items "center"}])]
    (d/div
      {:className ["card-actions" $layout]}
      (d/div
        {:className "wrapper"}

        (c/children props)))))


(defnc card
  [props]
  (let [$card (css
                {:position "relative"
                 :display "flex"
                 :flex-direction "column"
                 :max-width "300px"
                 :min-width "180px"
                 :padding "10px 10px 5px 10px"
                 :background-color "#eaeaea"
                 :border-radius "5px"
                 :transition "box-shadow .2s ease-in-out"}
                ["& .card-actions" {:opacity "0"
                                   :transition "opacity .4s ease-in-out"}]
                ["&:hover .card-actions" {:opacity "1"}]
                ;;
                ["& .avatar"
                 {:position "absolute"
                  :left "-10px"
                  :top "-10px"
                  :transition "all .1s ease-in-out"}]
                ["&:hover" {:box-shadow "1px 4px 11px 1px #ababab"}])]
    (d/div
      {:className $card}
      (c/children props))))


(def components
  (merge
    {:row row
     :card card
     :card/action card-action
     :card/actions card-actions
     :identity identity
     :avatar avatar
     :column column
     :checkbox checkbox
     :button button
     :buttons buttons
     :simplebar simplebar
     :calendar/timestamp timestamp-calendar
     :calendar/year-dropdown calendar-year-dropdown
     :calendar/month-dropdown calendar-month-dropdown
     :calendar/month calendar-month}
    #:input {:autosize autosize-input
             :idle idle-input}))
