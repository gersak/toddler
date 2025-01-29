(ns toddler.ui.fields
  (:require
   clojure.set
   [clojure.string :as str]
   [vura.core :as vura]
   [goog.string :as gstr]
   [shadow.css :refer [css]]
   [goog.string.format]
   [helix.core
    :refer [$ defnc provider <>]]
   [helix.dom :as d]
   [helix.children :as c]
   [helix.hooks  :as hooks]
   [toddler.input
    :as input
    :refer [TextAreaElement
            IdleInput]]
   [toddler.mask :refer [use-mask]]
   [toddler.core :as toddler :refer [use-translate]]
   [toddler.ui.elements :as e]
   ; [toddler.ui.elements.calendar
   ;  :refer [calendar period-calendar]]
    ; [toddler.icons :as icon]
   [toddler.material.outlined :as outlined]
   [toddler.dropdown :as dropdown
    :refer [use-dropdown]]
   [toddler.multiselect
    :refer [use-multiselect]]
   [toddler.ui :as ui]
   [toddler.popup :as popup]))

(def $field
  (css
   :flex
   {:flex "100 0 auto"
    :width "100px"}
   ["& > .content" :w-full]
   ["& .avatar"
    :border
    :border-normal-
    {:background-color "var(--avatar-bg)"
     :border-radius "20px"}]
   ["&.error" :color-negative]
   ["&.error .toddler-field-label" :color-negative]
   ["&.error .toddler-input-field" :border-negative]))

(def $field-wrapper
  (css
   :flex
   :items-center
   :rounded-md
   :cursor-text
   :grow
   {:transition "all .3s ease-in-out"}))

(def $clear
  (css
   ["& .clear" :text-transparent :cursor-pointer
    {:transition "color .3s ease-in-out"}]
   ["&:hover .clear" :text-neutral-400]
   ["& .clear:hover" :text-neutral-600]))

(def $label
  (css
   :select-none
   :uppercase
   :font-semibold
   :text-normal
   :block
   :h-5 :pl-1
   {:color "var(--field-label)"
    :font-size "10px"}))

(def $input-field
  (css
   :flex
   :grow
   :items-center
   :relative
   :bg-normal
   :border
   :border-normal
   :text-normal
   :text-sm
   {:transition "background-color .4s ease-in-out"
    :min-height "40px"
    :background-color "var(--field-bg)"
    :color "var(--field-text)"
    :border-color "var(--field-border)"}
   ["& .icon" :w-6 :h-6 :ml-2 :user-no-select]
   ["& .icon.right" :flex :items-center :justify-center :cursor-pointer :mr-1]
   ["&:focus-within"
    :animate-border-click
    {:background-color "var(--field-bg-active)"
     :border-color "var(--field-border-active)"}]
   ["&:focus-within .decorator" :text-click]
   ["&:hover:not(.disabled):not(:focus-within)"
    {:background-color "var(--field-bg-hover)"
     :border-color "var(--field-border-hover)"}]
    ;;
   :rounded-md
   ["& .avatar" :ml-2 :bg-avatar]
   ["& .avatar img" :rounded-sm]
   ["& .decorator" {:width "24px" :height "24px"
                    :margin-right "0.25rem"
                    :transition "color .3s ease-in-out, transform .3s ease-in-out"}]
   ["&:focus-within .decorator" {:transform "rotate(180deg)"}]
   ["& textarea" {:resize "none"}]
   ["& input, & textarea" :grow :px-3]
   ["& input[type=\"password\"]" {:font-family "Verdana" :letter-spacing "0.125em"}]
   ["& input:focus, & textarea:focus" {:color "var(--field-text-active)"}]
   ["& input::placeholder, & textarea::placeholder" :text-normal :font-medium {:user-select "none" :font-style "normal"}]))

(defnc textarea-field
  [{:keys [onChange on-change disabled placeholder error
           className class] :as props}]
  (let [onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))
        _input (hooks/use-ref nil)]
    (d/div
     {:class (toddler/conj-prop-classes
              ["toddler-field" $field (when error "error")]
              props)}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]
          :onClick (fn [] (.focus @_input))}
         (:name props)))
      (d/div
       {:class ["toddler-input-field"
                (when disabled "disabled")
                (when placeholder "show-placeholder")
                $input-field]}
       ($ TextAreaElement
          {:ref _input
           :placeholder placeholder
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           :onChange (fn [text] (onChange text))
           & (dissoc props :error :onChange :on-change :name :className :style :label :placeholder)}))))))

(defnc input-field
  [{:keys [onChange on-change disabled placeholder value style error] :as props}]
  (let [_input (hooks/use-ref nil)
        [_value set-local-value!] (hooks/use-state (or value ""))
        onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))]
    (hooks/use-effect
      [value]
      (when-not (= _value value)
        (set-local-value! (or value ""))))
    (d/div
     {:class ["toddler-field" $field (when error "error")]
      :style style}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]
          :onClick (fn [] (.focus @_input))}
         (:name props)))
      (d/div
       {:class ["toddler-input-field"
                (when disabled "disabled")
                $input-field]}
       (d/input
        {:ref _input
         :value _value
         :placeholder placeholder
         :autoComplete "off"
         :autoCorrect "off"
         :spellCheck "false"
         :autoCapitalize "false"
         :onChange (fn [^js e]
                     (let [value (.. e -target -value)]
                       (set-local-value! value)
                       (when (ifn? onChange)
                         (onChange value))))
         & (dissoc props :value :onChange :on-change :error
                   :name :className :style :label :placeholder)}))))))

(defnc password-field
  [{:keys [onChange on-change disabled placeholder value] :as props}]
  (let [_input (hooks/use-ref nil)
        [_value set-local-value!] (hooks/use-state (or value ""))
        [visible? set-visible!] (hooks/use-state false)
        onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))]
    (hooks/use-effect
      [value]
      (when-not (= _value value)
        (set-local-value! (or value ""))))
    (d/div
     {:class ["toddler-field" $field]}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]
          :onClick (fn [] (.focus @_input))}
         (:name props)))
      (d/div
       {:class ["toddler-input-field"
                (when disabled "disabled")
                $input-field]}
       (d/input
        {:ref _input
         :value _value
         :type (if visible? "text" "password")
         :placeholder placeholder
         :autoComplete "new-password" ;"off"
         :autoCorrect "off"
         :spellCheck "false"
         :autoCapitalize "false"
         :onChange (fn [^js e]
                     (let [value (.. e -target -value)]
                       (set-local-value! value)
                       (when (ifn? onChange)
                         (onChange value))))
         & (dissoc props :value :onChange :on-change :type
                   :name :className :style :label :placeholder)})
       (d/div
        {:className "icon right"
         :onClick (fn [] (set-visible! not))}
        ($ (if visible? outlined/visibility outlined/visibility-off))))))))

(defnc idle-field
  [{:keys [onChange placeholder on-change icon] :as props}]
  (let [_input (hooks/use-ref nil)
        onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))]
    (d/div
     {:class ["toddler-input-field"
              $input-field]}
     (when icon ($ icon {:className "icon"}))
     ($ IdleInput
        {:ref _input
         :autoComplete "off"
         :autoCorrect "off"
         :spellCheck "false"
         :autoCapitalize "false"
         :onChange (fn [text] (onChange text))
         & (dissoc props :icon :onChange :name :className :style :label)})
     (when (:name props)
       (d/label
        {:class ["toddler-field-label"]
         :onClick (fn [] (.focus @_input))}
        (:name props))))))

(letfn [(text->number [text]
          (cond
            (number? text) text
            (empty? text) nil
            :else
            (let [number (js/parseInt  text)]
              (when-not (js/Number.isNaN number) (int number)))))]
  (defnc integer-field
    [{:keys [onChange on-change disabled value style className]
      :as props}]
    (let [_input (hooks/use-ref nil)
          translate (use-translate)
          [focused? set-focused!] (hooks/use-state false)
          onChange (hooks/use-memo
                     [onChange on-change]
                     (or onChange on-change identity))
          [local set-local!] (hooks/use-state (str value))
          show (if value
                 (or
                  (if focused?
                    (or local "")
                    (translate (text->number local)))
                  "")
                 "")]
      (hooks/use-effect
        [local]
        (let [local-value (text->number local)]
          (when-not (= local-value value)
            (when (ifn? onChange)
              (onChange local-value)))))
      (hooks/use-effect
        [value]
        (when (not= value local) (set-local! (str value))))
      (d/div
       {:class ["toddler-field" $field]}
       (d/div
        {:className "content"}
        (when (:name props)
          (d/label
           {:class ["toddler-field-label" $label]
            :onClick (fn [] (.focus @_input))}
           (:name props)))
        (d/div
         {:class ["toddler-input-field"
                  (when disabled "disabled")
                  $input-field]}
         (d/input
          {:ref _input
           :value show
           :type "text"
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           :onFocus #(set-focused! true)
           :onBlur #(set-focused! false)
           :onChange (fn [^js e]
                       (let [text (.. e -target -value)]
                         (if (empty? text)
                           (set-local! nil)
                           (when (text->number text)
                             (set-local! text)))))
           & (dissoc props :value :onChange :on-change :onBlur :onFocus
                     :name :className :style :label)})))))))

(letfn [(text->number [text]
          (cond
            (number? text) text
            (empty? text) nil
            :else
            (let [number (js/parseFloat text)]
              (when-not (js/Number.isNaN number) (float number)))))]
  (defnc float-field
    [{:keys [onChange on-change disabled value] :as props}]
    (let [_input (hooks/use-ref nil)
          translate (use-translate)
          [focused? set-focused!] (hooks/use-state false)
          onChange (hooks/use-memo
                     [onChange on-change]
                     (or onChange on-change identity))
          [local set-local!] (hooks/use-state value)
          show (if value
                 (if focused?
                   (or local "")
                   (translate (text->number local)))
                 "")]
      (hooks/use-effect
        [local]
        (let [local-value (text->number local)]
          (when-not (= local-value value)
            (when (ifn? onChange)
              (onChange local)))))
      (d/div
       {:class ["toddler-field" $field]}
       (d/div
        {:className "content"}
        (when (:name props)
          (d/label
           {:class ["toddler-field-label" $label]
            :onClick (fn [] (.focus @_input))}
           (:name props)))
        (d/div
         {:class ["toddler-input-field"
                  (when disabled "disabled")
                  $input-field]}
         (d/input
          {:ref _input
           :value show
           :type "text"
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           :onFocus #(set-focused! true)
           :onBlur #(set-focused! false)
           :onChange (fn [^js e]
                       (let [text (.. e -target -value)]
                         (if (empty? text) (set-local! nil)
                             (when (text->number text)
                               (let [polished (-> text
                                                  (str/replace #"\," ".")
                                                  (str/replace #"\.+" ".")
                                                  (str/replace #"\.(?=.*\.)" "")
                                                  (str/replace #"\s+" ""))]
                                 (set-local! polished))))))
           & (dissoc props :value :onChange :onBlur :onFocus
                     :name :className :style :label)})))))))

(defn get-width
  [area]
  (when @area (.-width (.getBoundingClientRect ^js @area))))

(def $dropdown-popup (css :shadow-md :rounded-lg))

(defnc dropdown-field
  [{:keys [disabled] :as props}]
  (let [{:keys [input area toggle!] :as dropdown}
        (use-dropdown
         (->
          props
          (dissoc :className)))
        width (get-width area)]
    (provider
     {:context dropdown/*dropdown*
      :value dropdown}
     (d/div
      {:class ["toddler-field" $field]}
      (d/div
       {:className "content"}
       (when (:name props)
         (d/label
          {:class ["toddler-field-label" $label]
           :on-click (fn [] (when @input (.focus @input)))}
          (:name props)))
       ($ popup/Area
          {:ref area
           :on-click (fn []
                       (when-not disabled
                         (toggle!)
                         (when @input (.focus @input))))
           :class ["toddler-input-field"
                   (when disabled "disabled")
                   $input-field]}
          ($ dropdown/Popup
             {:style {:width width}
              :class ["dropdown-popup" $dropdown-popup]}
             ($ e/dropdown-wrapper
                {:width width}
                ($ dropdown/Options
                   {:render e/dropdown-option})))
          ($ dropdown/Input
             {:className (css :flex :grow)
              :autoComplete "off"
              :autoCorrect "off"
              :spellCheck "false"
              :autoCapitalize "false"
              & (dissoc props :on-change :onChange :name :className :style :label)})
          ($ outlined/keyboard-arrow-down {:className "decorator"})))))))

(def $multiselect-options
  (css
   :flex
   :flex-row
   :grow
   :flex-wrap
   :user-no-select
   {:gap "0.375rem 0.375rem"
    :padding "0.5rem 1rem 1rem 1rem"}))

(def $multiselect-field
  (css
   :flex
   :grow
   :items-center
   :relative
   :border
   :text-sm
   :pl-3
   :pr-8
   :flex-wrap
   :py-2
   {:transition "background-color .4s ease-in-out"
    :min-height "40px"
    :gap "0.375rem 0.375rem"
    :background-color "var(--field-bg)"
    :color "var(--field-text)"
    :border-color "var(--field-border)"}
   ["&:focus-within"
    :animate-border-click
    {:background-color "var(--field-bg-active)"
     :border-color "var(--field-border-active)"}]
   ["& .decorator"
    :top-2 :right-2 :absolute
    {:width "24px" :height "24px"
     :transition "color .3s ease-in-out, transform .3s ease-in-out"}]
   ["&:focus-within .decorator" :text-click {:transform "rotate(180deg)"}]
   ["&:hover:not(.disabled):not(:focus-within)"
    {:background-color "var(--field-bg-hover)"
     :border-color "var(--field-border-hover)"}]
    ;;
   :rounded-md
   :border-normal
   ["& .avatar" :mr-2 {:background-color "var(--avatar-bg)"}]
   ["& .avatar img" :rounded-sm]
   ["& input" {:flex "1 1 auto" :min-height "26px"}]
   ["& input::placeholder"
    {:user-select "none"
     :font-style "normal"}]))

(defnc multiselect-option
  {:wrap [(ui/forward-ref)]}
  [{:keys [value class className selected on-remove onRemove] :as props} _ref]
  (let [on-remove (some #(when (fn? %) %) [on-remove onRemove])
        {:keys [search-fn context-fn]
         :or {search-fn identity}} (hooks/use-context dropdown/*dropdown*)]
    (d/div
     {:ref _ref
      :class (toddler/conj-prop-classes
              ["toddler-multiselect-option"
               e/$tag
               (css
                ["& .remove" :ml-3]
                ["& .remove:hover" :color-negative])
               (when (ifn? context-fn)
                 (when-some [context (context-fn value)]
                   (name context)))
               (when selected "selected")]
              props)
      & (dissoc props :value :context :className :on-remove :onRemove)}
     (if-some [children (c/children props)]
       children
       (search-fn value))
     (when (ifn? on-remove)
       ($ outlined/close
          {:className "remove"
           :onClick (fn [e]
                      (when (ifn? on-remove) (on-remove value)))})))))

(defnc multiselect-field
  [{:keys [search-fn context-fn on-change onChange placeholder]
    render-option :render/option
    :or {search-fn str
         render-option multiselect-option}
    :as props}]
  (let [{:keys [open!
                options
                new-fn
                area
                search value]
         :as multiselect}
        (use-multiselect
         (assoc props
           :search-fn search-fn
           :context-fn context-fn))
        ;;
        on-change (or onChange on-change)
        width (get-width area)
        available-options (hooks/use-memo
                            [value]
                            (clojure.set/difference
                             (set options)
                             (set value)))]
    (provider
     {:context dropdown/*dropdown*
      :value multiselect}
     (d/div
      {:class (toddler/conj-prop-classes
               ["toddler-field" $field]
               props)}
      (d/div
       {:className "content"}
       (when (:name props)
         (d/label
          {:class ["toddler-field-label" $label]}
          (:name props)))
       ($ popup/Area
          {:ref area
           :class ["toddler-identity-multiselect"
                   (when-not (or (not-empty value) (not-empty search))
                     "empty")
                   $multiselect-field]
           :onClick open!}
          (<>
           (when (not-empty available-options)
             ($ dropdown/Popup
                {:class ["dropdown-popup" $dropdown-popup]}
                ($ e/dropdown-wrapper
                   {:style {:width width}}
                   (d/div
                    {:class [$multiselect-options]}
                    ($ toddler.multiselect/Options
                       {:render render-option})))))
           (map
            (fn [option]
              ($ render-option
                 {:key (search-fn option)
                  :value option
                  :onRemove #(on-change
                              (vec
                               (remove
                                (fn [_option] (= _option option))
                                value)))}))
            (:value props))
           (when (or new-fn (not-empty available-options))
             ($ dropdown/Input
                {& multiselect
                 :placeholder placeholder}))
           ($ outlined/keyboard-arrow-down {:className "decorator"}))))))))

(defnc timestamp-time
  [{:keys [value onChange on-change]}]
  (let [on-change (or onChange on-change)
        {:keys [hour minute] :as state}
        (hooks/use-memo
          [value]
          (if-not value {:hour 0 :minute 0}
                  (->
                   value
                   vura/time->value
                   vura/day-time-context)))
        ;;
        props'
        (use-mask
         {:value (gstr/format "%02d:%02d" hour minute)
          :mask (gstr/format "%02d:%02d" 0 0)
          :delimiters #{\:}
          :constraints [#"([0-1][0-9])|(2[0-3])" #"[0-5][0-9]"]
          :onChange (fn [time-]
                      (let [[h m] (map js/parseInt (str/split time- #":"))]
                        (when (ifn? on-change)
                          (on-change
                           (->
                            state
                            (assoc :hour h :minute m)
                            vura/context->value
                            vura/value->time)))))})]
    (d/input
     {:spellCheck false
      :auto-complete "off"
      :onChange (fn [])
      & (select-keys props' [:ref :value :on-key-down])})))

(def $timestamp-field
  (css
   :flex :flex-col
   ["& .row"
    :flex :text-sm
    {:gap "0.25rem"
     :color "var(--field-text)"}]
   ["& .row .date, & .row .time"
    :border
    {:background-color "var(--field-bg)"
     :border-color "var(--field-border)"}]
   ["& .row .date" {:min-width "10rem"}]
   ["& .row .time" {:max-width "5.5rem"}]
   ["& .row .date, & .row .time"
    :flex :items-center
    :relative :transition :grow :h-10
    :rounded-md
    :cursor-pointer
    {:transition "background-color .4s ease-in-out, border-color .4s ease-in-out"
     :min-height "40px"
     :color "var(--field-text)"
     :border-color "var(--field-border)"}]
   ["& .row .date:focus-within, & .row .time:focus-within"
    {:background-color "var(--field-bg-active)"
     :border-color "var(--field-border-active)"
     :animation-name "var(--input-normal-click)"
     :animation-duration ".5s"}]
   ["& .row .date svg, & .row .time svg"
    :absolute
    :h-4 :w-4
    :right-2
    #_:top-2]
   ["& .row .date:hover:not(:focus-within), & .row .time:hover:not(:focus-within)"
    {:border-color "var(--field-border-hover)"}]
   ["& .row .date:hover:not(:focus-within) svg, & .row .time:hover:not(:focus-within) svg" :text-hover]
   ["& .row input" {:max-width "6rem"} :cursor-pointer :ml-3]
   ["& .calendar" :mt-1]))

(defnc timestamp-field
  [{value :value
    :keys [onChange on-change dropdown? time?]
    :as props
    :or {dropdown? true
         time? true}}]
  (let [translate (toddler/use-translate)
        on-change  (or onChange on-change)
        _area (hooks/use-ref nil)
        _popup (hooks/use-ref nil)
        [show-dropdown? toggle!] (hooks/use-state nil)
        width 300]
    (popup/use-outside-action
     show-dropdown? _area _popup
     #(toggle! false))
    (d/div
     {:className "toddler-field"}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]}
         (:name props)))
      (d/div
       {:class [$timestamp-field]}
       (d/div
        {:className "row"}
        ($ popup/Area
           {:ref _area}
           (d/div
            {:className "date"
             :on-click (fn [] (toggle! true))}
            (d/input
             {:read-only true
              :value (try
                       (if value
                         (translate value :medium-date)
                         (translate :not-available))
                       (catch js/Error e
                         (.log js/console e)
                         ""))})
            (if (nil? value)
              ($ outlined/calendar-month)
              ($ outlined/close
                 {:onClick (fn [] (on-change nil))})))
           (when show-dropdown?
             ($ popup/Element
                {:ref _popup
                 :style {:width width}
                 :class ["dropdown-popup" $dropdown-popup]}
                ($ e/dropdown-wrapper
                   {:max-height "400px"
                    :width width}
                   ($ ui/calendar
                      {:value value
                       :on-change (fn [v]
                                    (toggle! false)
                                    (when (ifn? on-change)
                                      (on-change v)))})))))
        (when time?
          (d/div
           {:className "time"}
           ($ timestamp-time
              {:value value
               :on-change (fn [value] (on-change value))})
           ($ outlined/schedule))))
       (when-not dropdown?
         ($ ui/calendar
            {& (select-keys props [:value :onChange :on-change])})))))))

(defnc date-field
  [props]
  ($ timestamp-field
     {:format :full-date
      :time? false
      & props}))

(def $period-field
  (css
   :flex :flex-col
   ["& .inputs" :text-sm {:color "var(--field-text)"}]
   ["& .inputs .from-row, & .inputs .to-row"
    :flex
    {:gap "0.25rem"}]
   ["& .inputs .date, & .inputs .time"
    :border
    {:background-color "var(--field-bg)"
     :border-color "var(--field-border)"}]
   ["& .inputs .to-row" :mt-1]
   ["& .inputs.no-time" {:max-width "100px"}]
   ["& .inputs .date, & .inputs .time"
    :flex :items-center :rounded-md
    :relative :transition :grow :h-10
    :cursor-pointer
    {:transition "background-color .4s ease-in-out, border-color .4s ease-in-out"
     :min-height "40px"
     :color "var(--field-text)"
     :border-color "var(--field-border)"}]
   ["& .inputs .date:focus-within, & .inputs .time:focus-within, &.opened .inputs .date"
    {:background-color "var(--field-bg-active)"
     :border-color "var(--field-border-active)"
     :animation-name "var(--input-normal-click)"
     :animation-duration ".5s"}]
   ["& .inputs .date svg, & .inputs .time svg"
    :absolute
    :h-4 :w-4
    :right-2]
   ["&:not(.opened) .inputs .date:hover:not(:focus-within), &:not(.opened) .inputs .time:hover:not(:focus-within)"
    {:border-color "var(--field-border-hover)"}]
   ["& .inputs .date:hover:not(:focus-within) svg, & .inputs .time:hover:not(:focus-within) svg" :color-hover]
   ["& .inputs input"  :cursor-pointer :ml-3]
   ["& .inputs.no-time input" {:max-width "134px"}]
   ["& .inputs:not(.no-time) input" {:max-width "6rem"}]
   ["& .calendar" :mt-1]
   #_["& .inputs .date"]))

(defnc timestamp-period-field
  [{[start end] :value
    :keys [onChange on-change dropdown? time?]
    :as props
    :or {dropdown? true
         time? true}}]
  (let [translate (toddler/use-translate)
        on-change  (or onChange on-change)
        _area (hooks/use-ref nil)
        _popup (hooks/use-ref nil)
        [show-dropdown? toggle!] (hooks/use-state nil)
        width 300]
    (popup/use-outside-action
     show-dropdown? _area _popup
     #(toggle! false))
    (d/div
     {:className "toddler-field"}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]}
         (:name props)))
      ($ popup/Area
         {:ref _area}
         (d/div
          {:class [$period-field (when show-dropdown? "opened")]}
          (d/div
           {:className (str "inputs" (when-not time? " no-time"))}
           (d/div
            {:className "from-row"}
            (d/div
             {:className "date"
              :onClick (fn [] (toggle! true))}
             (d/input
              {:read-only true
               :value (if start
                        (translate start :date)
                        (translate :not-available))})
             (if start
               ($ outlined/close
                  {:onClick (fn [] (on-change [nil end]))})
               ($ outlined/calendar-month)))
            (when time?
              (d/div
               {:className "time"}
               ($ timestamp-time
                  {:value start
                   :on-change (fn [start] (on-change [start end]))})
               ($ outlined/schedule))))
           (d/div
            {:className "to-row"}
            (d/div
             {:className "date"
              :onClick (fn [] (toggle! true))}
             (d/input
              {:read-only true
               :value (if end
                        (translate end :date)
                        (translate :not-available))})
             (if end
               ($ outlined/close
                  {:onClick (fn [] (on-change [start nil]))})
               ($ outlined/calendar-month)))
            (when time?
              (d/div
               {:className "time"}
               ($ timestamp-time
                  {:value end
                   :on-change (fn [end] (on-change [start end]))})
               ($ outlined/schedule)))))
          (when-not dropdown?
            ($ ui/calendar-period
               {& (select-keys props [:value :onChange :on-change])}))
          (when show-dropdown?
            ($ popup/Element
               {:ref _popup
                :style {:width width}
                :class ["dropdown-popup" $dropdown-popup]}
               ($ e/dropdown-wrapper
                  {:max-height "30rem"
                   :width width}
                  ($ ui/calendar-period
                     {& (select-keys props [:value])
                      :on-change (fn [v]
                                   (when (ifn? on-change)
                                     (on-change v)))}))))))))))

(defnc date-period-field
  [{[start end] :value
    :keys [onChange on-change dropdown?]
    :as props
    :or {dropdown? true}}]
  (let [translate (toddler/use-translate)
        on-change  (or onChange on-change)
        _area (hooks/use-ref nil)
        _popup (hooks/use-ref nil)
        [show-dropdown? toggle!] (hooks/use-state nil)
        width 300]
    (popup/use-outside-action
     show-dropdown? _area _popup
     #(toggle! false))
    (d/div
     {:className "toddler-field"}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]}
         (:name props)))
      ($ popup/Area
         {:ref _area}
         (d/div
          {:class [$period-field (when show-dropdown? "opened")]}
          (d/div
           {:class [(css :flex :gap-1
                         ["& input" {:max-width "120px"}])
                    "inputs"
                    "no-time"]}
           (d/div
            {:className "date"
             :onClick (fn [] (toggle! true))}
            (d/input
             {:read-only true
              :value (if start
                       (translate start :medium-date)
                       (translate :not-available))})
            (if start
              ($ outlined/close
                 {:onClick (fn [] (on-change [nil end]))})
              ($ outlined/calendar-month)))
           (d/div
            {:className "date"
             :onClick (fn [] (toggle! true))}
            (d/input
             {:read-only true
              :value (if end
                       (translate end :medium-date)
                       (translate :not-available))})
            (if end
              ($ outlined/close
                 {:onClick (fn [] (on-change [start nil]))})
              ($ outlined/calendar-month))))
          (when-not dropdown?
            ($ ui/calendar-period
               {& (select-keys props [:value :onChange :on-change])}))
          (when show-dropdown?
            ($ popup/Element
               {:ref _popup
                :style {:width width}
                :class ["dropdown-popup" $dropdown-popup]}
               ($ e/dropdown-wrapper
                  {:max-height "30rem"
                   :width width}
                  ($ ui/calendar-period
                     {& (select-keys props [:value])
                      :on-change (fn [v]
                                   (when (ifn? on-change)
                                     (on-change v)))}))))))))))

(def $boolean-field
  (css
   :cursor-pointer
   :flex
   :grow
   :items-center
   :relative
   :border
   :text-sm
   :justify-between
   {:transition "background-color .4s ease-in-out"
    :min-height "40px"
    ; :background-color "var(--field-bg)"
    :background-color "var(--field-bg)"
    ; :color "var(--field-text)"
    :color "var(--field-label)"
    :border-color "var(--field-border)"}
   ["&:hover" :border-hover {:background-color "var(--field-bg-hover)"}]
   ["& .verbal" :cursor-pointer :grow :text-field-normal
    :select-none :px-2 {:min-width "5rem"}]
   ["& .figurative" :pr-2]
   ["& .figurative svg" :w-5 :h-5]))

(defnc boolean-field
  [{:keys [value
           onChange
           on-change]
    :as props}]
  (let [on-change (or on-change onChange)
        translate (toddler/use-translate)]
    (d/div
     {:class [(css
               :cursor-pointer
               :pl-2
               {:color "var(--field-label)"}
               ["& .toddler-checkbox-wrapper" :flex :items-center]
               ["& .toddler-checkbox-wrapper:hover" :text-hover]
               ["& .toddler-checkbox-wrapper > .figurative > svg" :h-5 :w-5]
               ["& .toddler-checkbox-wrapper > .verbal"
                :text-xxs :font-semibold :uppercase :ml-1
                :select-none])]}
     (d/div
      {:className "toddler-checkbox-wrapper"
       :onClick (fn [] (on-change not))}
      (d/div
       {:className "figurative"}
       ($ (case value
            true outlined/check-box
            outlined/check-box-outline-blank)))
      (d/div
       {:className "verbal"}
       (str (translate (:name props))))))))

(defnc copy-field
  [{:keys [value] :as props}]
  (d/div
   {:class ["toddler-field"
            $field]}
   (d/div
    {:className "content"}
    (when (:name props)
      (d/label
       {:class ["toddler-field-label" $label]}
       (:name props)))
    (d/div
     {:class ["toddler-input-field"
              $boolean-field]
      :onClick (fn [] (.writeText js/navigator.clipboard (str value)))}
     (d/span
      {:className "verbal"
       :style {:overflow "hidden"
               :text-overflow "ellipsis"}}
      value)
     (d/div
      {:className "figurative"}
      ($ outlined/content-copy))))))

(defnc checklist-field
  [_])

(defnc identity-field
  [{:keys [disabled] :as props}]
  (let [{:keys [value input area toggle!] :as dropdown}
        (use-dropdown
         (->
          props
          (assoc :search-fn :name)
          (dissoc :className)))
        width (get-width area)]
    (provider
     {:context dropdown/*dropdown*
      :value dropdown}
     (d/div
      {:class ["toddler-field" $field]}
      (d/div
       {:className "content"}
       (when (:name props)
         (d/label
          {:class ["toddler-field-label" $label]
           :onClick (fn [] (when @input (.focus @input)))}
          (:name props)))
       ($ popup/Area
          {:ref area
           :onClick (fn [] (toggle!) (when @input (.focus @input)))
           :class ["toddler-input-field"
                   (when disabled "disabled")
                   $input-field]}
          ($ dropdown/Popup
             {:style {:width width}
              :class ["dropdown-popup" $dropdown-popup]}
             ($ e/dropdown-wrapper
                {:width width}
                ($ dropdown/Options
                   {:render e/identity-dropdown-option})))
          (d/div
           {:className "avatar"}
           ($ ui/avatar {:size 18 & value}))
          ($ dropdown/Input
             {:className (css :flex :grow)
              :autoComplete "off"
              :autoCorrect "off"
              :spellCheck "false"
              :autoCapitalize "false"
              & (dissoc props :onChange :name :className :style :label)})
          ($ outlined/keyboard-arrow-down {:className "decorator"})))))))

(defnc IdentityMultiselectOption
  {:wrap [(ui/forward-ref)]}
  [{{:keys [name] :as option} :value :as props} _ref]
  ($ multiselect-option
     {:ref _ref
      & props}
     ($ ui/avatar
        {:size 18
         :className "avatar"
         & option})
     (d/div {:className "name"} name)))

(defnc identity-multiselect-field
  [{:keys [search-fn disabled on-change onChange placeholder
           options-not-available-message]
    render-option :render/option
    :or {search-fn str
         options-not-available-message "Options not available"
         render-option IdentityMultiselectOption}
    :as props}]
  (let [{:keys [open!
                options
                new-fn
                area input
                search value]
         :as multiselect} (use-multiselect
                           (assoc props :search-fn search-fn))
        ;;
        on-change (or onChange on-change)
        ;;
        width (get-width area)
        translate (toddler/use-translate)
        [focused? focused!] (hooks/use-state false)
        [_focused? toggle-focused!] (toddler/use-idle
                                     focused?
                                     (fn [v]
                                       (case v
                                         true (focused! true)
                                         (focused! false))))
        available-options (hooks/use-memo
                            [value]
                            (clojure.set/difference
                             (set options)
                             (set value)))]
    (letfn [(focus! [_] (toggle-focused! true))
            (blur! [_] (toggle-focused! false))]
      (hooks/use-effect
        :once
        (when-some [el @input]
          (.addEventListener el "focus" focus!)
          (.addEventListener el "blur" blur!)
          (fn []
            (.removeEventListener el "focus" focus!)
            (.removeEventListener el "blur" blur!)))))
    (provider
     {:context dropdown/*dropdown*
      :value multiselect}
     (d/div
      {:class ["toddler-field" $field]}
      (d/div
       {:className "content"}
       (when (:name props)
         (d/label
          {:class ["toddler-field-label" $label]}
          (:name props)))
       ($ popup/Area
          {:ref area
           :class ["toddler-identity-multiselect"
                   (when-not (or (not-empty value) (not-empty search))
                     "empty")
                   (when disabled "disabled")
                   $multiselect-field]
           :onClick (fn []
                      (open!))}
          (<>
           (when (not-empty available-options)
             ($ dropdown/Popup
                {:class ["dropdown-popup" $dropdown-popup]}
                ($ e/dropdown-wrapper
                   {:style {:width width}}
                   (d/div
                    {:class [$multiselect-options]}
                    ($ toddler.multiselect/Options
                       {:render IdentityMultiselectOption})))))
           (map
            (fn [option]
              ($ render-option
                 {:key (search-fn option)
                  :value option
                  :onRemove #(on-change
                              (vec
                               (remove
                                (fn [_option] (= _option option))
                                value)))}))
            (:value props))
           (when (or new-fn (not-empty available-options))
             ($ dropdown/Input
                {& multiselect
                 :placeholder placeholder}))
           ($ outlined/keyboard-arrow-down {:className "decorator"}))))))))

(defnc currency-field
  [{:keys [disabled onChange value name
           onFocus on-focus className
           read-only]
    :or {onChange identity}}])

(defnc search-field
  [{:keys [onChange on-change disabled placeholder value] :as props}]
  (let [_input (hooks/use-ref nil)
        [_value set-local-value!] (hooks/use-state (or value ""))
        onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))]
    (hooks/use-effect
      [value]
      (when-not (= _value value)
        (set-local-value! (or value ""))))
    (d/div
     {:class ["toddler-field"
              $field]}
     (d/div
      {:className "content"}
      (when (:name props)
        (d/label
         {:class ["toddler-field-label" $label]
          :onClick (fn [] (.focus @_input))}
         (:name props)))
      (d/div
       {:class ["toddler-input-field"
                (when disabled "disabled")
                $input-field]}
       ($ IdleInput
          {:ref _input
           :value _value
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           :onChange (fn [^js e]
                       (let [value (.. e -target -value)]
                         (set-local-value! value)
                         (when (ifn? onChange)
                           (onChange value))))
           & (dissoc props :value :onChange :on-change
                     :name :className :style :label)}))))))

(def components
  #:field {:idle idle-field
           :text textarea-field
           :boolean boolean-field
           :copy copy-field
           :checklist checklist-field
           :input input-field
           :password password-field
           :integer integer-field
           :float float-field
           :currency currency-field
           :dropdown dropdown-field
           :multiselect multiselect-field
           :timestamp timestamp-field
           :date date-field
           :date-period date-period-field
           :timestamp-period timestamp-period-field
           :identity identity-field
           :identity-multiselect identity-multiselect-field})
