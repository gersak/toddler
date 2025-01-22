(ns toddler.ui.elements
  (:require
   ["react" :as react]
   [clojure.set :as set]
   [clojure.core.async :as async]
   [clojure.string :as str]
   [shadow.css :refer [css]]
   #_[toddler.avatar
      :refer [avatar]
      :as avatar]
   [toddler.ui :as ui]
   [toddler.util :as util]
   [helix.core
    :refer [$ defnc <> provider create-context]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [toddler.avatar :refer [avatar]]
   [toddler.input
    :refer [AutosizeInput
            IdleInput]]
   [toddler.material.outlined :as outlined]
   [toddler.core :refer [use-delayed] :as toddler]
   [toddler.dropdown :as dropdown]
   [toddler.multiselect :as multiselect]
   [toddler.scroll :as scroll]
   [toddler.ionic :as ionic]
   [toddler.popup :as popup]
   [toddler.layout :as layout]
   [toddler.ui.elements.calendar :refer [calendar period-calendar]]
   [toddler.provider :refer [UI]]))

(defnc simplebar
  {:wrap [(ui/forward-ref)]}
  [{:keys [className hidden]
    show-shadow? :shadow
    :as props}
   _ref]
  (let [$default (css
                  {:transition "box-shadow 0.3s ease-in-out"}
                  ["&.shadow-top:before" {:z-index "100" :opacity "1 !important"}]
                  ["&.shadow-bottom:after" {:z-index "100" :opacity "1 !important"}])
        [shadow set-shadow!] (hooks/use-state #{})
        ; $shadow-top (css)
        ; $shadow-bottom (css)
        $hidden (css ["& .simplebar-track" {:display "none"}])
        className (str/join
                   " "
                   (cond-> [className $default]
                     (shadow :top) (conj "shadow-top")
                     (shadow :bottom) (conj "shadow-bottom")
                     hidden (conj $hidden)))
        local-ref (react/useRef nil)
        _ref (or _ref local-ref)]
    (hooks/use-effect
      :once
      (when show-shadow?
        (when-some [el (.-current _ref)]
          (letfn [(init-shadow []
                    (let [client-height (.-clientHeight el)
                          scroll-height (.-scrollHeight el)]
                      (when (and
                             (>= scroll-height client-height)
                             (not (contains? shadow :bottom)))
                        (set-shadow! conj :bottom))))
                  (track-shadow []
                    (let [scroll-top (.-scrollTop el)
                          scroll-height (-
                                         (.-scrollHeight el)
                                         (.-clientHeight el))]
                      (cond
                        ;;
                        (zero? scroll-top)
                        (set-shadow!
                         (fn [shadow]
                           (if (contains? shadow :top)
                             (disj shadow :top)
                             shadow)))
                        ;;
                        (>= (+ scroll-top 10) scroll-height)
                        (set-shadow!
                         (fn [shadow]
                           (if (contains? shadow :bottom)
                             (disj shadow :bottom)
                             shadow)))
                        ;;
                        (and
                         (> scroll-top 0)
                         (not (>= scroll-top scroll-height)))
                        (set-shadow!
                         (fn [shadow]
                           (if (not= shadow #{:top :bottom})
                             #{:top :bottom}
                             shadow)))
                        ;;
                        (> scroll-top 0)
                        (set-shadow!
                         (fn [shadow]
                           (if (not (contains? shadow :top))
                             (conj shadow :top)
                             shadow)))
                        ;;
                        (= shadow #{}) nil
                        ;;
                        :else (set-shadow! #{}))))]
            (init-shadow)
            (.addEventListener el "scroll" track-shadow)
            (fn []
              (.removeEventListener el "scroll" track-shadow))))))
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
  [{:keys [class className] :as props}]
  (let [$wrapper (css
                  :flex
                  :border
                  :border-normal
                  {:min-height "2em"}
                  ["& button" :px-2 {:min-width "6em"}]
                  ["& button:hover" :color++]
                  ["& button:not(:last-child)" :border-r :border-normal]
                  :button:first-of-type {:border-top-left-radius "4px"
                                         :border-bottom-left-radius "4px"}
                  :button:last-of-type {:border-top-right-radius "4px"
                                        :border-bottom-right-radius "4px"})]
    (d/div
     {:class (cond-> ["toddler-buttons" $wrapper]
               (string? class) (conj class)
               (string? className) (conj className)
               (vector? class) (into class))}
     (c/children props))))

(def $button
  (css
   :flex
   :text-xs
   ; :border
   ; :border-normal
   :rounded-md
   :justify-center
   :items-center
   :px-2
   ; :py-2
   :leading-loose
   :mx-2
   :my-2
   :button-neutral
   :animate-border-click
   :font-semibold
   ["&:hover" :button-neutral-hover]
   {:transition "all .2s ease-in"}
   {:justify-content "center"
    :min-height "2rem"
    :min-width "80px"
    :cursor "pointer"
    :user-select "none"}
    ;; default
   ["&:hover" :border-highlighted]
    ;;
   ["&.positive" :button-positive]
   ["&.positive:hover" :button-positive-hover]
    ;;
   ["&.negative" :button-negative]
   ["&.negative:hover" :button-negative-hover]
   ;;
   ["&[disabled]" :button-disabled]))

(defnc button
  {:wrap [(ui/forward-ref)]}
  [{:keys [class className] :as props} _ref]
  (d/div
   {:ref _ref}
   (d/button
    {:class (cond-> [$button]
              (string? class) (conj class)
              (sequential? class) (into class)
              (string? className) (conj className))
     & (dissoc props :class :className)}
    (c/children props))))

(def $checkbox
  (css
   :flex
   :text-sm
   :select-none
   :cursor-pointer
   :justify-center
   :items-center
   :relative
   :py-3
   :body-text-md
   :text-normal
   {:transition "color .3s ease-in-out"}
   ["& .icon" :mr-1 :w-4 :h-4]
    ; ["&.selected" ]
   ["&[disabled]" :cursor-default]))

(defnc checkbox
  [{:keys [className class value] :as props}]
  (d/div
   {:className className
    :class (cond->
            [$checkbox]
             value (conj "selected")
             (string? class) (conj class)
             (sequential? class) (concat class))
    & (dissoc props :className :class)}
   ($ (if value outlined/check-box outlined/check-box-outline-blank)
      {:className "icon"})
   (c/children props)))

; (defnc checkbox
;   [{:keys [value disabled] :as props}]
;   (let [$checkbox (css
;                     :cursor-pointer
;                     :w-4
;                     :h-4
;                     :flex
;                     :justify-center
;                     :items-center
;                     :outline-none
;                     ["& path" :cursor-pointer]
;                     ["&:active" :border-transparent])
;         $active (css :text-neutral-600)
;         $inactive (css :text-neutral-300)
;         $disabled (css :pointer-events "none")]
;     (d/button
;       {:class [$checkbox
;                (if value $active $inactive)
;                (when disabled $disabled)]
;        & (dissoc props :value)}
;       ($ (case value
;            nil ionic/checkmark
;            ionic/checkmark)))))

(def $checklist
  (css
   ["& .row .name" :text-neutral-400]
   ["& .row.selected .name, & .row:hover .name" :text-neutral-600]))

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
   (d/div {:class "name"} cname)))

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
                  #{value}))]
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

(def $row
  (css
   ["&.toddler-labeled-row"
    :flex :flex-col]
   ["&.toddler-labeled-row > .toddler-row-label"
    :font-bold
    :m-1
    {:text-transform "uppercase"
     :font-size "1em"}]
   ["&.toddler-row, & .toddler-row"
    :m-1
    {:display "flex"
     :flex-direction "row"
     :align-items "center"
     :flex-grow "1"}]))

(defnc row
  {:wrap [(ui/forward-ref)]}
  [{:keys [className label position style align] :as props} _ref]
  (let [position (or position align)
        $layout (css
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
                         :text-normal
                         :font-bold
                         ; :m-1
                         :select-none
                         {:text-transform "uppercase"
                          :font-size "0.75rem"}])}
       (d/div
        {:className "label"}
        (d/label label))
       (d/div
        {:ref _ref
         :style style
         :class [$layout
                 $position
                 "toddler-row"
                 className]}
        (c/children props)))
      (d/div
       {:ref _ref
        :style style
        :class [$layout
                $position
                "toddler-row"
                className]}
       (c/children props)))))

(def $column
  (css
   :flex
   :flex-col
   :grow
   :stretch
   ["& > .toddler-column-label"
    {:margin "4px 4px 4px 4px"
     :padding-bottom "2px"
     :text-transform "uppercase"
     :font-size "0.75rem"}]))

#_(defnc column
    {:wrap [(ui/forward-ref)]}
    [{:keys [label position className] :as props} _ref]
    (let [$layout (css
                   {:display "flex"
                    :flex-direction "column"
                    :flex-grow "1"}
                   ["& > .label"
                    {:margin "4px 4px 4px 4px"
                     :padding-bottom "2px"
                     :text-transform "uppercase"
                     :font-size "1em"}])
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
                $position
                "toddler-column"
                className]}
       (when label
         (d/div
          {:className "label"}
          (d/label label)))
       (c/children props))))

(defnc column
  {:wrap [(ui/forward-ref)]}
  [{:keys [label position align class className] :as props
    {:keys [width] :as style} :style} _ref]
  (let [$layout (css
                 {:display "flex"
                  :flex-direction "column"
                  :flex-grow "1"}
                 ["& > .toddler-column-label"
                  :select-none
                  :font-bold
                  {:margin "4px 4px 4px 4px"
                   :padding-bottom "2px"
                   :text-transform "uppercase"
                   :font-size "0.75em"}])
        $start (css {:justify-content "flex-start"})
        $center (css {:justify-content "center"
                      :align-items "center"})
        $end (css {:justify-content "flex-end"})
        $explode (css {:justify-content "space-between"})
        $position (case position
                    :center $center
                    :end $end
                    :explode $explode
                    $start)]
    (d/div
     {:ref _ref
      :style style
      :class (cond->
              [$layout
               $position
               "toddler-column"]
               (string? className) (conj className)
               (string? class) (conj class)
               (not-empty class) (into class))}
     (when label
       (d/div
        {:className "toddler-column-label"}
        (d/label label)))
     (c/children props))))

(def $dropdown-wrapper
  (css
   :relative
   :flex
   :flex-col
   :m-0
   :rounded-md
   :border
   {:background-color "var(--dropdown-bg)"
    :border-color "var(--dropdown-border)"}
   ["& .simplebar-scrollbar:before"
    :pointer-events-none
    {:max-height "400px"}]
   ["& .toddler-dropdown-option"
    :p-2
    :flex
    :justify-start
    :items-center
    :cursor-pointer
    :rounded-sm
    :text-sm
    :text-normal
    :border-t
    :border-normal
    {:background "transparent"
     :font-size "0.875rem"
     :transition "color .2s ease-in,background-color .2s ease-in"}]
    ;;
   ["& .toddler-dropdown-option .avatar"
    :rounded-sm
    :mr-2]
    ;;
   ["& .toddler-dropdown-option:hover"
    :color++]
    ;;
   ["& .toddler-dropdown-option.selected"
    :color+]
    ;;
   ["& .toddler-dropdown-option:first-child"
    {:border-top "none"}]))

(let [default-max-height (* 6 40)]
  (defnc dropdown-wrapper
    [{:keys [style class className width max-height] :as props
      :or {max-height default-max-height}}]
    (let [[_content {content-width :width}] (toddler/use-dimensions)]
      (<>
       (d/div
        {:style {:width 10000
                 :height 1000
                 :top -10000
                 :left -10000
                 :display "flex"
                 :visibility "hidden"
                 :position "absolute"}}
        (d/div
         (d/div
          {:ref #(reset! _content %)
           :class (cond-> ["dropdown-wrapper" $dropdown-wrapper
                           "dropdown-wrapper-track"
                           (css
                            ["& .simplebar-content" :flex :flex-col]
                            ["& .simplebar-content-wrapper" :pr-2])]
                    (string? className) (conj className)
                    (vector? class) (into class)
                    (string? class) (conj class))
           & (dissoc props :class :className :max-height)}
          (c/children props))))
       (when @_content
         (d/div
          {:class (cond-> ["dropdown-wrapper" $dropdown-wrapper
                           (css
                            ["& .simplebar-content" :flex :flex-col])]
                    (string? className) (conj className)
                    (vector? class) (into class)
                    (string? class) (conj class))
           & (dissoc props :class :className :max-height)}
          (if max-height
            ($ simplebar
               {:style (merge
                        {:max-height max-height}
                        {:width (or width content-width)}
                        style)}
               (c/children props))
            (d/div
             {:style (merge
                      {:width (or width content-width)}
                      style)}
             (c/children props)))))))))

(defnc dropdown-option
  {:wrap [(ui/forward-ref)]}
  [{:keys [selected] :as props} _ref]
  (d/div
   {:class (cond->
            ["toddler-dropdown-option"]
             selected (conj "selected"))
    :ref _ref
    & (dissoc props :value)}
   (c/children props)))

(defnc dropdown
  [props]
  (let [{:keys [toggle!
                area]
         :as dropdown}
        (dropdown/use-dropdown props)]
    (provider
     {:context dropdown/*dropdown*
      :value dropdown}
     ($ popup/Area
        {:ref area
         :onClick (fn [] (toggle!))
         & (select-keys props [:class :className])}
        ($ dropdown/Input
           {& (select-keys props [:placeholder])})
        ($ dropdown/Popup
           {:className "dropdown-popup"}
           ($ dropdown-wrapper
              ($ dropdown/Options
                 {:render dropdown-option})))))))

(def $tag
  (css
   :flex
   :justify-center
   :items-center
   :select-none
   :text-xxxs
   :uppercase
   :font-semibold
   {:padding-left "12px"
    :padding-right "12px"
    :min-height "26px"
    :border-radius "0.25rem"}
   ; ["& svg" :ml-2 :pr-1]
   :text-xs
   :color
   :border
   :border-normal
   :cursor-pointer
   {:color "var(--tag-color-normal)"
    :background-color "var(--tag-bg-normal)"
    :border-color "var(--tag-border-normal)"}
   ["& .remove" :w-4 :h-4]
   ["&:hover" {:border-color "var(--tag-border-normal-hover)"}]
   ["& .avatar" :mr-2 :rounded-sm :bg-normal-highlighted]
   ["&.negative" {:color "var(--tag-color-negative)"
                  :background-color "var(--tag-bg-negative)"
                  :border-color "var(--tag-border-negative)"}]
   ["&.negative:hover" {:border-color "var(--tag-border-negative-hover)"}]
   ["&.positive" {:color "var(--tag-color-positive)"
                  :background-color "var(--tag-bg-positive)"
                  :border-color "var(--tag-border-positive)"}]
   ["&.positive:hover" {:border-color "var(--tag-border-positive-hover)"}]
   ["&.fun" {:color "var(--tag-color-negative)" :background-color "var(--tag-bg-negative)"}]
   ["&.selected" :border-normal+]
   ["& .remove:hover" {:color "var(--tag-normal-remove-hover)"}]
   ["&.negative .remove:hover" {:color "var(--tag-negative-remove-hover)"}]
   ["&.positive .remove:hover" {:color "var(--tag-positive-remove-hover)"}]))

(def $multiselect-wrapper
  (css
   {:display "flex"
    :justify-content "row"
    :align-items "center"
    :min-height "2.3em"}))

(defnc multiselect-wrapper
  [props]
  (d/div
   {:class $multiselect-wrapper}
   (c/children props)))

(defnc MultiselectInput
  [props]
  ($ UI
     {:components {:input autosize-input
                   :wrapper multiselect-wrapper}}
     ($ dropdown/Input
        {& props}
        (c/children props))))

;; TODO - this isn't full implemented
(defnc multiselect
  [props]
  (d/div
   {:class (css :flex :items-center)}
   ($ multiselect/Element
      {& props})))

(def $tooltip
  (css
   :box-border
   :justify-center
   :items-center
   :shadow-md
   :border
   :text-normal
   :border-normal
   :select-none
   {:opacity "0"
    :color "var(--tooltip-color)"
    :background-color "var(--tooltip-bg)"
    :border "1px solid var(--tooltip-border) "
    :border-radius "5px"
    :padding-left  "1rem"
    :padding-right "1rem"
    :padding-top "0.375rem"
    :padding-bottom "0.375rem"
    :font-size "0.625rem"
    :font-weight "600"}
   ["&.success, &.positive"
    {:background-color "var(--tooltip-positive)"
     :color "var(--tooltip-positive-text)"
     :border-color "var(--tooltip-positive-border)"}]
   ["&.warn, &.warning,  &.danger"
    {:background-color "var(--tooltip-warn)"
     :color "var(--tooltip-warn-text)"
     :border-color "var(--tooltip-warn-border)"}]
   ["&.error, &.negative"
    {:background-color "var(--tooltip-negative)"
     :color "var(--tooltip-negative-text)"
     :border-color "var(--tooltip-negative-border)"}]
   ["&.exception"
    {:background-color "var(--tooltip-exception)"
     :color "var(--tooltip-exception-text)"
     :border-color "var(--tooltip-exception-border)"}]
   ["& pre" {:font-size "0.625rem"
             :font-weight "600"}]
   ["&.show" {:transform "scale(1)" :opacity "1"}]))

(defnc tooltip
  {:wrap [(ui/forward-ref)]}
  [{:keys [message preference disabled className class offset]
    :or {preference popup/cross-preference
         offset 8}
    :as props} ref]
  (let [[visible? set-visible!] (hooks/use-state nil)
        [_ change-visible!] (toddler/use-idle
                             visible?
                             (fn [x]
                               (set-visible!
                                (case x
                                  :NULL false
                                  x))))
        ; visible? true
        hidden? (use-delayed (not visible?) 300)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)
        _popup (or ref popup)]
    (if (and (not disabled) (some? message))
      ($ popup/Area
         {:ref area
          :className "tooltip-popup-area"
          :onMouseLeave (fn [] (change-visible! false))
          :onMouseEnter (fn []
                          (change-visible! true)
                          #_(when-not (some
                                       #(= % @_popup)
                                       (util/get-dom-parents (.-target e)))
                              (change-visible! true)))}
         (c/children props)
         (when (or visible? (not hidden?))
           ($ popup/Element
              {:ref _popup
               :class (cond->
                       ["tooltip" $tooltip]
                        ;;
                        (not visible?) (conj "hide")
                        ;;
                        visible? (conj "show")
                        ;;
                        (some? className) (conj className)
                        ;;
                        (string? class) (conj class)
                        ;;
                        (sequential? class) (concat class))
               :style {:visibility (if hidden? "hidden" "visible")}
               :preference preference
               :offset offset}
              message)))
      (c/children props))))

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
  {:wrap [(ui/forward-ref)]}
  [props _ref]
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
               ["& .card-actions"
                {:opacity "0"
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
     {:ref _ref
      :className $card}
     (c/children props))))

(defnc identity-dropdown-option
  {:wrap [(ui/forward-ref)]}
  [{:keys [value] :as props} ref]
  ($ dropdown-option
     {:ref ref
      & (dissoc props :ref :value)}
     ($ avatar
        {:size 18
         :className "avatar"
         & value})
     (:name value)))

(defnc Drawer
  [{:keys [opened? class className width position]
    :or {width "22.5em" position :right}
    :as props}]
  (let [{container-height :height} (layout/use-container-dimensions)
        [_drawer drawer-dimensions] (toddler/use-dimensions)]
    (d/div
     {:ref _drawer
      :class (cond->
              ["toddler-drawer"
               (if opened? "opened" "closed")]
               className (conj className)
               (string? class) (conj class)
               (sequential? class) (into class))
      :style (cond->
              {:width width
               :height container-height
               :transform (str "translateX(" (when (= position :left) "-") (if opened? "0em" width) ")")}
               (= position :right) (assoc :right "0")
               (= position :left) (assoc :left "0"))}
     (provider
      {:context layout/*container-dimensions*
       :value drawer-dimensions}
      (c/children props)))))

(def $close
  (css
   :h-5 :w-5
   :text-neutral-600
   :cursor-pointer
   {:opacity "0.5"
    :transition "opacity .3s ease-in-out"}
   ["&:hover" {:opacity "1"}]))

(defnc close
  [props]
  ($ ionic/close
     {:class $close
      & props}))

(defonce tabs-context (create-context))
(defonce actions-context (create-context))

(def $tabs
  (css
   :flex
   :justify-between
   :relative
   :pb-2
   ["& .tabs" :flex :flex-wrap :px-2]))

(def $tab
  (css
   :uppercase
   :z-10
   :h-6
   :px-3
   :py-2
   :flex
   :items-center
   :justify-center
   :select-none
   :cursor-pointer
   :text-normal
   :text-xxs
   :rounded-md
   :font-semibold
   {:min-width "5em"}
   ; ["&:first-child" :ml-8]
   ["&:hover"
    {:text-decoration "none"
     :color "var(--tab-hover-color)"
     ; :background-color "var(--tab-hover-bg)"
     }]
   ["&.selected"
    {:color "var(--tab-selected-color)"
     ; :background-color "var(--tab-selected-bg)"
     }]))

(defnc tabs
  {:wrap [(ui/forward-ref)]}
  [{:keys [class className]
    :as props} _ref]
  (let [;on-change (or on-change onChange)
        tabs-target (hooks/use-ref nil)
        _tabs (hooks/use-ref nil)
        _tabs (or _tabs _ref)
        [selected on-select!] (hooks/use-state nil)
        [available set-available!] (toddler/use-idle
                                    nil (fn [tabs]
                                          (on-select!
                                           (fn [id]
                                             (when-not (= tabs :NULL)
                                               (if-not (nil? id) id
                                                       (ffirst tabs))))))
                                    {:initialized? true})
        register (hooks/use-callback
                   [selected]
                   (fn register
                     ([tab] (register tab tab nil))
                     ([tab order] (register tab tab order))
                     ([id tab order]
                      (set-available!
                       (fn [tabs]
                         (vec (sort-by #(nth % 2) (conj tabs [id tab order]))))))))
        unregister (hooks/use-callback
                     [selected]
                     (fn [tab]
                       (set-available!
                        (fn [tabs]
                          (vec
                           (sort-by
                            #(nth % 2)
                            (remove
                             (fn [[_ _tab _]]
                               (= tab _tab))
                             tabs)))))))
        update-tab (hooks/use-callback
                     [selected]
                     (fn [key tab order]
                       (set-available!
                        (fn [tabs]
                          (let [next (mapv
                                      (fn [[k t o]]
                                        (if (= key k) [k tab order]
                                            [k t o]))
                                      tabs)]
                            next)))))
        tabs (map #(take 2 %) available)
        container-dimensions (layout/use-container-dimensions)
        [_ {tabs-height :height}] (toddler/use-dimensions _tabs)
        tab-content-dimensions  (hooks/use-memo
                                  [(:height container-dimensions) tabs-height]
                                  (assoc container-dimensions :height
                                         (- (:height container-dimensions)
                                            tabs-height)))
        translate (toddler/use-translate)
        tab-elements (hooks/use-ref nil)
        ;;
        {marker-top :top
         marker-left :left
         marker-height :height
         marker-width :width}
        (hooks/use-memo
          [selected]
          (if-not selected
            {:top 0 :left 0}
            (if-some [selected-el (get @tab-elements selected)]
              (let [[left top width height] (util/dom-dimensions selected-el)
                    [tabs-left tabs-top] (util/dom-dimensions @_tabs)
                    top (- top tabs-top)
                    left (- left tabs-left)]
                {:top top :left left
                 :width width :height height})
              {:top 0 :left 0})))]
    (<>
     (d/div
      {:ref _tabs
       :class (cond->
               (list "toddler-tabs" $tabs)
                className (conj className)
                (string? class) (conj class)
                (sequential? class) (into class))}
      (d/div
       {:ref #(swap! tab-elements assoc ::marker %)
        :style {:top marker-top :left marker-left
                :width marker-width :height marker-height}
        :className (css
                    :bg-normal-
                    :z-0
                    :absolute
                    :rounded-md
                    {:transition "width .2s ease-in-out, height .2s ease-in-out, left .2s ease-in-out"})})
      (d/div
       {:ref #(reset! tabs-target %)
        :class ["tabs"]}
       (map
        (fn [[id tab]]
          (d/div
           {:key tab
            :ref #(swap! tab-elements assoc id %)
            :class (cond->
                    (list "toddler-tab" $tab)
                     (= id selected) (conj "selected")
                     className (conj className)
                     (string? class) (conj class)
                     (sequential? class) (into class))
            :on-click (fn [] (on-select! id))}
           (if (string? tab) tab
               (translate tab))))
        tabs)))
     (provider
      {:context tabs-context
       :value {:register register
               :unregister unregister
               :update update-tab
               :select! on-select!
               :selected selected}}
      (provider
       {:context layout/*container-dimensions*
        :value tab-content-dimensions}
       (d/div
        {:className "tab-content"}
        (c/children props)))))))

(defnc tab
  [{:keys [name tab id focus? position] :as props
    :or {id tab}}]
  (let [tab (or name tab)
        {:keys [select!
                selected
                register
                unregister
                update]} (hooks/use-context tabs-context)]
    (hooks/use-effect
      :once
      (register id tab position)
      (when focus?
        (async/go
          (async/<! (async/timeout 1000))
          (select! id)))
      (fn []
        (unregister id)))
    (hooks/use-effect
      [tab]
      (update id tab position))
    (when (= id selected)
      (c/children props))))

(def $tab-action
  (css
   :uppercase
   :h-6
   :flex
   :items-center
   :justify-center
   :select-none
   :cursor-pointer
   :text-normal
   :border-b
   :border-l
   :border-r
   :border-normal
   :font-semibold
   :text-xxs
   {:min-width "5em"
    :padding-right "10px"}
   ["& .icon" :h-3 :w-3 {:margin-left "6px"}]
   ["& .name" {:margin-left "6px"}]
   ["&:first-child" :mr-4]
   ["&[disabled]" :pointer-events-none :color-]
   ["&:hover" :bg-normal-focused :color++ {:text-decoration "none"}]))

(defnc action
  [{:keys [icon name className class disabled]
    _tooltip :tooltip :as props}]
  (d/div
   {:disabled (when disabled true)
    :class (cond->
            ["action-wrapper" $tab-action]
             className (conj className)
             (string? class) (conj class)
             (sequential? class) (into class))
    & (dissoc props :icon :name :className :class :tooltip)}
   (when icon
     (d/div {:className "icon"} ($ icon)))
   (when name
     (d/div {:className "name"} name))))

(defnc identity-option
  {:wrap [(ui/forward-ref)]}
  [{:keys [name type selected? on-click] :as option} _ref]
  (d/div
   {:class ["option"
            (when type (clojure.core/name type))
            (when selected? "selected")]
    :on-click on-click}
   (d/div {:className "avatar"} #_($ ui/avatar {:size 18 & option}))
   (d/div {:className "user"} name)))

(def $identity-option
  (css
   ["& .option"
    :flex
    :items-center
    :text-normal
    :text-xxs
    :select-none
    {:border "1px solid var(--border-normal)"
     :min-height "1.5rem"
     :border-radius "1px"
     :padding-left "3px"
     :padding-right "8px"}]
   ["& .option.PERSON, & .option.ACCESS" {:border-radius "16px"}]
   ["& .option .avatar"
    :overflow-hidden
    :bg-normal-highlighted
    {:width "18px" :height "18px" :border-radius "1px" :margin-right "6px"}]
   ["& .option.selected" {:border-color "var(--border-highlighted)"}]
   ["& .option.PERSON .avatar, & .option.ACCESS .avatar" {:border-radius "50px"}]))

(defnc relation-picker
  [{:keys [options selected on-change search-fn]
    :or {selected []
         search-fn :name}}]
  (let [search-height 40
        [search set-search!] (hooks/use-state nil)
        {container-height :height} (layout/use-container-dimensions)
        options-height (- container-height search-height)
        translate (toddler/use-translate)
        displayed-options (let [selected? (set (map :euuid selected))]
                            (as-> options o
                              ;;
                              (if (empty? search) o
                                  (let [pattern (re-pattern
                                                 (apply str "(?i)" (str/replace search #"\s+" ".*")))]
                                    (filter #(re-find pattern (search-fn %)) o)))
                              ;;
                              (map
                               (fn [option]
                                 (cond-> option
                                   (selected? (:euuid option)) (assoc :selected? true)))
                               o)))
        $selectable (css
                     ["& .option" :cursor-pointer])]
    (<>
     (d/div
      {:className (css
                   :text-normal
                   :relative
                   ["& .actions"
                    :absolute
                    :flex
                    :flex-end
                    :z-10
                    {:top "0px" :right "1rem"}]
                   ["& .actions .action" :box-action :p-1 :cursor-pointer]
                   ["& .actions .action:hover" :box-action-hover]
                   ["& .actions .action.selected" :box-action-selected]
                   ["& .actions .action svg" :h-4 :w-4]
                   ["& .options" :pt-1 :flex])}
      (d/div
       {:className "actions"}
       (<>
        (d/div
         {:className "action"
          :onClick (fn [] (on-change options))}
         ($ outlined/done-all))
        (d/div
         {:className "action"
          :onClick (fn [] (on-change nil))}
         ($ outlined/delete-forever)))))
     (d/div
      {:className (css
                   :overflow-hidden
                   :p-6
                   :z-20
                   :text-normal
                   :text-sm
                   {:transition "height .2s ease-in-out"})
       :style {:height search-height}}
      (d/input
       {:placeholder (translate :search)
        :onChange #(set-search! (.. % -target -value))
        :value (or search "")}))
     ($ ui/simplebar
        {:style {:max-height options-height
                 :min-height "10rem"}}
        (d/div
         {:class [(css
                   :py-8
                   :px-8
                   :flex :flex-wrap {:gap "0.5rem"})
                  $selectable
                  $identity-option]}
         (map
          (fn [{:keys [euuid selected?] :as option}]
            ($ identity-option
               {:key euuid & option
                :on-click (fn []
                            (when (ifn? on-change)
                              (let [next (if selected?
                                           (vec
                                            (remove
                                             (fn [o]
                                               (= (:euuid o) (:euuid option)))
                                             selected))
                                           (conj selected option))]
                                (on-change next))))}))
          displayed-options))))))

(def components
  (merge
   {:row row
    :close close
    :tabs tabs
    :tab tab
    :action action
    :card card
    :tooltip tooltip
    :drawer Drawer
    :popup/area popup/Area
    :popup/element popup/Element
    :card/action card-action
    :card/actions card-actions
    :identity identity
    :avatar avatar
    ; :avatar/editor avatar/editor
    :column column
    :checkbox checkbox
    :button button
    :dropdown dropdown
    :buttons buttons
    :simplebar simplebar
    :calendar calendar
    :calendar/period period-calendar}
   #:input {:autosize autosize-input
            :idle idle-input}))

(comment
  (get components :modal/avatar-editor))
