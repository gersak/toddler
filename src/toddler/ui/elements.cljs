(ns toddler.ui.elements
  (:require
    [clojure.set :as set]
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
    [toddler.provider :refer [ExtendUI UI]]))


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
        & (cond->
            (-> props
                (dissoc :shadow :className :class)
                (update :style scroll/transform-style))
            _ref (assoc :scrollableNodeProps #js {:ref _ref}))}
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


(def $button
  (css 
    :flex
    :font-extrabold
    :border-2
    :border-transparent
    :rounded-sm
    :justify-center
    :items-center
    :px-4
    :py-4
    :leading-loose
    :mx-3
    :my-2
    {:transition "all .2s ease-in"}
    {:justify-content "center"
     :max-height "30px"
     :min-width "80px"
     :font-size "1em"
     :cursor "pointer"
     :user-select "none"}
    ;; default
    :text-neutral-600
    ["&:hover"
     :text-neutral-700
     {:text-shadow "0px 0px 12px #b3b3b3"}]
    ;;
    ["&.positive:hover"
     :text-green-600
     {:text-shadow "0px 0px 12px #2bff3d"}]
    ;;
    ["&.negative:hover"
     :text-red-500
     {:text-shadow "0px 0px 12px #ff8989"}]
    ["&.fun:hover"
     :text-cyan-600
     {:text-shadow "0px 0px 12px #06b7d4"}]
    ;;
    ["&.fresh:hover" {:color "#ff4ed9"
                      :text-shadow "0px 0px 12px #ff4ed9"}]
    ;;
    ["&.stale:hover" {:color "#686eba"
                      :text-shadow "0px 0px 12px #686eba"}]
    ;;
    ["&[disabled]" :text-neutral-400 :cursor-default :pointer-events-none]))


(defnc button
  [{:keys [context]
   :or {context "default"} :as props}]
  (d/button
    {:class [$button (name context)] 
     & props}
    (c/children props)))


(defnc checkbox
  [{:keys [value disabled] :as props}]
  (let [$checkbox (css
                    :cursor-pointer
                    :w-4
                    :h-4
                    :flex
                    :justify-center
                    :items-center
                    :outline-none
                    ["& path" :cursor-pointer]
                    ["&:active" :border-transparent])
        $active (css :text-neutral-600)
        $inactive (css :text-neutral-300)
        $disabled (css :pointer-events "none")]
    (d/button
      {:class [$checkbox
               (if value $active $inactive)
               (when disabled $disabled)]
       & (dissoc props :value)}
      #_($ (case value
           nil icon/checkboxDefault
           icon/checkbox)))))


(defnc checklist-row
  [{cname :name 
    value :value 
    disabled :disabled
    onChange :onChange}]
  (d/div
    {:class ["row"
             (when value "selected")
             (when disabled "disabled")]
     :onClick #(onChange (not value))}
    ; (d/div 
    ;   {:class "icon"}
    ;   ($ icon/checkbox))
    (d/div 
      {:class "name"}
      cname)))


(defnc checklist [{:keys [value
                          options
                          multiselect?
                          onChange
                          className] 
                   :or {onChange identity
                        value []}}]
  (let [value' (set/intersection
                 (set options)
                 (if multiselect? 
                   (set value)
                   #{value}))
        $checklist (css
                     ["& .row .name" :text-neutral-400]
                     ["& .row.selected .name, & .row:hover .name" :text-neutral-600])]
    (d/div
      {:class [className
               $checklist]}
      (d/div
        {:class "list"}
        (map
          (fn [{:keys [name] :as option}]
            ($ checklist-row
              {:key name
               :name name 
               :value (boolean (contains? value' option))
               :onChange #(onChange
                            (if (true? %)
                              (if multiselect?
                                ((fnil conj []) value' option) 
                                option)
                              (if multiselect?
                                (vec (remove #{option} value'))
                                nil)))}))
          options)))))


(defnc row
  [{:keys [className label position] :as props} _ref]
  {:wrap [(forward-ref)]}
  (let [$layout (css
                  :text-gray-800
                  :m-1
                  {:display "flex"
                   :flex-direction "row"
                   :align-items "center"
                   :flex-grow "1"})
        $start (css {:justify-content "flex-start"})
        $center (css {:justify-content "center"})
        $end (css {:justify-content "flex-end"})
        $explode (css {:justify-content "space-between"})
        $position (case position
                    :center $center
                    :end $end
                    :explode $explode
                    $start)]
    (if label
      (d/div
        {:className (css :flex :flex-col
                         ["& .label"
                          :text-neutral-400
                          :font-bold
                          :m-1
                          {:text-transform "uppercase"
                           :font-size "1em"}])}
        (d/div
          {:className "label"}
          (d/label label))
        (d/div
          {:ref _ref
           :class [$layout
                   $position
                   className]
           :position position}
          (c/children props)))
      (d/div
        {:ref _ref
         :class [$layout
                 $position
                 className]
         :position position}
        (c/children props)))))


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
  [props _ref]
  {:wrap [(forward-ref)]}
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
                  [:hover :text-neutral-600
                   {:background-color "#e2f1fc"}]
                  ["&:last-child" {:border-bottom "none"}])]
    (d/div
      {:class [$layout]
       :ref _ref
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


; (defnc calendar-month-dropdown
;   [{:keys [value] :as props}]
;   (let [$wrapper (css
;                    {:margin "5px 0"
;                     :cursor "pointer"}
;                    ["& input" :cursor-pointer :text-rose-700])
;         [area-position set-area-position!] (hooks/use-state nil)
;         ;;
;         months (range 1 13)
;         search-fn (date/use-calendar-months) 
;         ;;
;         {:keys [area toggle!] :as dropdown}
;         (dropdown/use-dropdown
;           (->
;             props
;             (assoc :area-position area-position
;                    :value (or value (vura/month? (vura/date)))
;                    :search-fn search-fn 
;                    :options months)
;             (dissoc :className)))]
;     (provider
;       {:context dropdown/*dropdown*
;        :value dropdown}
;       (provider
;         {:context popup/*area-position*
;          :value [area-position set-area-position!]}
;         ($ popup/Area
;            {:ref area
;             :onClick (fn [] (toggle!))
;             :className $wrapper}
;            ($ dropdown/Input
;               {:className (css :flex :w-28 {:text-align "right"})})
;            ($ dropdown/Popup
;               {:className "dropdown-popup"
;                :render/option dropdown-option
;                :render/wrapper dropdown-wrapper}))))))

; (defnc calendar-year-dropdown
;   [{:keys [value] :as props}]
;   (let [$wrapper (css
;                    {:margin "5px 0"
;                     :cursor "pointer"}
;                    ["& input" :cursor-pointer :text-rose-700])
;         [area-position set-area-position!] (hooks/use-state nil)
;         ;;
;         years (date/use-calendar-years)
;         ;;
;         {:keys [area toggle!] :as dropdown}
;         (dropdown/use-dropdown
;           (->
;             props
;             (assoc :area-position area-position
;                    :value (or value (vura/year? (vura/date)))
;                    :options years)
;             (dissoc :className)))]
;     (provider
;       {:context dropdown/*dropdown*
;        :value dropdown}
;       (provider
;         {:context popup/*area-position*
;          :value [area-position set-area-position!]}
;         ($ popup/Area
;            {:ref area
;             :onClick (fn [] (toggle!))
;             :className $wrapper}
;            ($ dropdown/Input
;               {:className (css :w-12) & props})
;            ($ dropdown/Popup
;               {:className "dropdown-popup"
;                :render/option dropdown-option
;                :render/wrapper dropdown-wrapper}))))))


(def $tag
  (css
    :rounded-sm
    :px-2
    :py-1
    :m-1
    :flex
    :justify-center
    :items-center
    :text-neutral-600
    ["& svg" :ml-2 :pr-1]
    ["& .remove"
     :cursor-pointer
     :flex
     :items-center
     :justify-center
     {:transition "color .2s ease-in"}]
    ;; Colors
    :text-neutral-100
    ; :bg-gray-600
    ; :font-semibold
    {:background-color "#344a6a"}
    ["& .remove:hover" :text-rose-400]
    ["& .remove" {:color "#647288"}]
    ["& .remove path" :cursor-pointer]
    ;;
    ["&.positive"
     :text-neutral-600
     {:background-color "#c4ed7c"}]
    ["&.positive .remove:hover" :text-black]
    ["&.positive .remove" :text-cyan-600]
    ;;
    ["&.negative"
     :text-neutral-50
     :bg-rose-400
     {:background-color "#d64242"}]
    ["&.negative .remove:hover" :text-black]
    ["&.negative .remove" {:color "#a10303"}]))


(defnc multiselect-option 
  [{:keys [context] :as props}]
  ($ multiselect/Option
    {:class [$tag (when context (name context))]
     & props}))


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


(defnc calendar-month-header
  [{:keys [className days]}]
  (let [$style (css
                 :flex
                 :flex-row
                 :cursor-default
                 {:cursor "default"}
                 ["& .day-wrapper .day" :text-neutral-600]
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


(defnc calendar-day
  [{:keys [value
           onClick
           class
           className]
    {:keys [day-in-month
            today
            first-day-in-month?
            last-day-in-month?
            disabled
            next-month
            prev-month
            period-start
            period-end
            picked
            selected
            weekend?]} :day}]
  (d/div
    {:onClick (when-not disabled onClick)
     :class (cond-> []
              (string? className) (conj className)
              (string? class) (conj class)
              (sequential? class) (into class)
              period-start (conj "period-start")
              period-end (conj "period-end")
              picked (conj "picked")
              next-month (conj "next-month")
              prev-month (conj "prev-month")
              selected (conj "selected")
              first-day-in-month? (conj "first-day-in-month")
              last-day-in-month? (conj "last-day-in-month")
              disabled (conj "disabled")
              today (conj "today")
              weekend? (conj "weekend")
              (nil? value) (conj "empty"))}
    (d/div
      {:class "day"}
      (d/div (or day-in-month " ")))))


(defnc calendar-month
  [{:keys [days on-select disabled read-only]}]
  (let [$month (css
                 :flex
                 :flex-col
                 :items-center
                 {:width "220px"})
        $week (css :flex :flex-row)
        weeks (sort-by key (group-by :week days))
        $day (css
               :border
               :border-transparent
               :text-neutral-600
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
                :text-neutral-800
                :font-bold]
               ;;
               ["& .day.weekend" :text-rose-700]
               ;;
               ["& .day.empty" {:cursor "default"}]
               ;;
               ["& .day.disabled, & .day:hover.disabled"
                :border
                :border-solid
                :border-transparent
                :text-neutral-400
                :cursor-default]
               ;;
               ["& .day:hover:not(.empty), & .day.selected"
                :text-white
                :border
                :rounded-sm
                :border-cyan-800
                :font-bold
                :text-neutral-100
                ; {:background-color "#5986bc"}
                {:background-color "#354a6a"}
                ])
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
                    ($ calendar-day
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


; (defnc timestamp-calendar
;   [{:keys [value onChange] :as props}]
;   (let [$style (css
;                  {:display "flex"
;                   :flex-direction "column"
;                   :border-radius "3px"
;                   :padding "7px"
;                   :width "230px"
;                   ; :height "190px"
;                   }
;                  ; (str popup/dropdown-container) {:overflow "hidden"}
;                  ["& .header-wrapper" {:display "flex" :justify-content "center" :flex-grow "1"}]
;                  ["& .header"
;                   :flex
;                   :grow
;                   :justify-between
;                   :w-24
;                   :h-14]
;                  ["& .header .years"
;                   {:position "relative"
;                    :display "flex"
;                    :align-items "center"}]
;                  ["& .header .months"
;                   {:position "relative"
;                    :display "flex"
;                    :align-items "center"}]
;                  ["& .content-wrapper"
;                   {:display "flex"
;                    :height "200px"
;                    :justify-content "center"
;                    :flex-grow "1"}])
;         ;;
;         {:keys [selected] :as props'}
;         (hooks/use-memo
;           [value onChange]
;           (let [v (vura/time->value (or value (vura/date)))
;                 context (vura/day-time-context v)]
;             (cond-> 
;               (merge props context)
;               (some? value) (assoc :selected v)
;               (fn? onChange) (assoc
;                                :onChange (fn [v]
;                                            (when-not (= v value)
;                                              (when v
;                                                (onChange (vura/value->time v)))))))))
;         ;;
;         {:keys [days]
;          {:keys [year month]} :state
;          {:keys [on-prev-month
;                  on-next-month
;                  on-year-change
;                  on-month-change
;                  on-day-change]} :events} (date/use-calendar props' :month)
;         upstream-is-selected (hooks/use-context date/*calendar-selected*)
;         is-selected (hooks/use-memo
;                       [selected]
;                       (if-not selected (constantly false)
;                         (let [selected-context (vura/day-context selected)]
;                           (fn [day]
;                             (date/same day selected-context)))))]
;     (d/div
;       {:className $style}
;       (d/div
;         {:className "header-wrapper"}
;         (d/div
;           {:className "header"}
;           (d/div
;             {:className "years"}
;             #_($ icon/previous
;                  {:onClick on-prev-month
;                   :className "button"})
;             ($ calendar-year-dropdown
;                {:value year
;                 :onChange on-year-change}))
;           #_(d/div
;               {:className "months"}
;               ($ calendar-month-dropdown
;                  {:value month
;                   :onChange on-month-change})
;               #_($ icon/next
;                    {:onClick on-next-month
;                     :className "button"}))))
;       (d/div
;         {:className "content-wrapper"}
;         (d/div
;           {:className "content"}
;           (provider
;             {:context date/*calendar-selected*
;              :value (or upstream-is-selected is-selected)}
;             ($ calendar-month
;                {:days days
;                 :on-select (fn [{:keys [day-in-month]}]
;                              (on-day-change day-in-month))
;                 & props})))))))


; (defnc timestamp-time
;   [{:keys [value read-only disabled onChange]}]
;   (let [{:keys [hour minute] :as state} (hooks/use-memo
;                                           [value]
;                                           (if-not value {:hour 0 :minute 0}
;                                             (->
;                                               value
;                                               vura/time->value
;                                               vura/day-time-context)))
;         props' (use-mask
;                  {:value (gstr/format "%02d:%02d" hour minute)
;                   :disabled disabled
;                   :read-only read-only
;                   :mask (gstr/format "%02d:%02d" 0 0)
;                   :delimiters #{\:}
;                   :constraints [#"([0-1][0-9])|(2[0-3])" #"[0-5][0-9]"]
;                   :onChange (fn [time-]
;                               (let [[h m] (map js/parseInt (str/split time- #":"))]
;                                 (when (ifn? onChange)
;                                   (onChange
;                                     (->
;                                       state 
;                                       (assoc :hour h :minute m)
;                                       vura/context->value
;                                       vura/value->time)))))})]
;     (d/div
;       {:className (css
;                     {:display "flex"
;                      :justify-content "center"
;                      :align-items "center"
;
;                      :font-size "1em"
;                      :margin "3px 0 5px 0"
;                      :justify-self "center"}
;                     ["& input" {:max-width "40px"}]
;                     ["& .time" {:outline "none"
;                                 :border "none"}])}
;       (d/input
;         {:className "time"
;          :spellCheck false
;          :auto-complete "off"
;          & (dissoc props' :className :constraints :delimiters :mask)}))))


; (defnc period-calendar
;   [{:keys [onChange]
;     [start end :as value] :value
;     :or {value [nil nil]}}]
;   (let [$wrapper (css :flex)
;         [after before] (hooks/use-memo
;                          [start end]
;                          (let [start (some->
;                                        start
;                                        vura/date->value
;                                        vura/midnight)
;                                end (some->
;                                      end
;                                      vura/date->value
;                                      vura/midnight
;                                      (+ vura/day))]
;                            [(fn [{:keys [value]}]
;                               (when start
;                                 (if end
;                                   (and (<= start value)
;                                        (< value end))
;                                   (<= start value))))
;                             (fn [{:keys [value]}]
;                               (when end
;                                 (if start
;                                   (and (<= start value)
;                                        (< value end))
;                                   (< value end))))]))]
;     (d/div
;       {:className $wrapper}
;       (provider
;         {:context date/*calendar-selected*
;          :value after}
;         ($ timestamp-calendar
;            {:key :start
;             :value start
;             :onChange (fn [v]
;                         (when (fn? onChange)
;                           (onChange (assoc value 0 v))))}))
;       (provider
;         {:context date/*calendar-selected*
;          :value before}
;         ($ timestamp-calendar
;            {:key :end
;             :value end
;             :onChange (fn [v]
;                         (when (fn? onChange)
;                           (onChange (assoc value 1 v))))})))))


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


(defnc identity-dropdown-option
  [{:keys [option] :as props} ref]
  {:wrap [(forward-ref)]}
  ($ dropdown-option
    {:ref ref
     & (dissoc props :ref :option)}
    ($ avatar {:size :small
                 :className (css :mr-2)
                 & option})
    (:name option)))


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
     ; :calendar/timestamp timestamp-calendar
     ; :calendar/year-dropdown calendar-year-dropdown
     ; :calendar/month-dropdown calendar-month-dropdown
     :calendar/month calendar-month}
    #:input {:autosize autosize-input
             :idle idle-input}))
