(ns toddler.ui.default.fields
  (:require
    clojure.set
    [clojure.string :as str]
    [shadow.css :refer [css]]
    [goog.string.format]
    [helix.core
     :refer [$ defnc provider]]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.hooks  :as hooks]
    [toddler.input
     :as input
     :refer [AutosizeInput
             NumberInput
             TextAreaElement]]
    [toddler.hooks :refer [use-translate]]
    [toddler.ui.default.elements :as e]
    [toddler.dropdown :as dropdown
     :refer [use-dropdown]]
    [toddler.multiselect
     :refer [use-multiselect]]
    [toddler.date :as date]
    [toddler.ui :as ui]
    [toddler.ui.provider :refer [ExtendUI]]
    [toddler.popup :as popup]
    ["toddler-icons$default" :as icon]
    ["react" :as react]))


(defnc field
  [{:keys [name className style]
    :as props}]
  (let [$style (css
                 :flex :flex-col
                 :mx-2 :my-1
                 ["& .field-name"
                  :text-gray-500
                  :text-sm
                  :font-bold
                  :uppercase
                  {:user-select "none"
                   :transition "all .3s ease-in-out"}])]
    (d/div
      {:class [className $style]
       :style style 
       & (select-keys props [:onClick])}
      (when name (d/label {:className "field-name"} name))
      (c/children props))))



(defnc field-wrapper
  [{:keys [className] :as props}]
  (let [$style (css
                 :flex
                 :items-center
                 :border-3
                 :border-gray-400
                 :rounded-md
                 :px-3 :py-1
                 :cursor-text
                 :text-gray-800
                 :bg-gray-200
                 {:transition "all .3s ease-in-out"
                  :min-height "2.25em"}
                 ["&:focus-within"
                  {:border-color "#2cc8c8 !important"
                   :box-shadow "0 0 3px #2cc8c8"
                   :background-color "transparent"}])]
  (d/div
    {:class [className
             $style]}
    (c/children props))))


(defnc textarea-field
  [props]
  (let [$style (css
                 ["& textarea" {:font-family "Robot"}])
        _input (hooks/use-ref nil)]
    ($ field
       {:className [$style]
        :onClick (fn [] (when @_input (.focus @_input)))
        & props}
       ($ field-wrapper
          {:className (css
                        :grow
                        :py-1
                        ["& textarea"
                         :text-gray-800
                         {:overflow "hidden"
                          :min-height "2em"
                          :border "none"
                          :resize "none"
                          :box-sizing "border-box"
                          :margin-top "6px" 
                          :padding "0"
                          :font-size "1em"}])}
          ($ TextAreaElement
             {:ref _input 
              :spellCheck false
              :auto-complete "off"
              :className "input"
              & (cond->
                  (->
                    props
                    (dissoc :name :style :className)
                    (update :value #(or % ""))))})))))


(defnc input-field
  [{:keys [onChange on-change] :as props}]
  (let [_input (hooks/use-ref nil)
        onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))
        $wrapper (css
                   {:min-height "2em"})]
    ($ field
       {:onClick (fn [] (when @_input (.focus @_input)))
        & props}
       ($ field-wrapper
          {:class [$wrapper]}
          ($ AutosizeInput
             {:ref _input
              :className (css
                            {:outline "none"
                             :border "none"})
              :autoComplete "off"
              :autoCorrect "off"
              :spellCheck "false"
              :autoCapitalize "false"
              :onChange (fn [^js e]
                          (onChange (.. e -target -value)))
              & (dissoc props :onChange :name :className :style)})))))


(defnc integer-field
  [{:keys [onChange]
    :or {onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ field
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ field-wrapper
          ($ NumberInput
             {:ref input
              :className [(css ["& input" :border-0])]
              & (->
                  props
                  (dissoc :name :className :style)
                  (assoc :onChange
                         (fn [e]
                           (some->>
                             (.. e -target -value)
                             not-empty
                             (re-find #"-?\d+[\.|,]*\d*")
                             (js/parseFloat)
                             onChange))))})))))


(defnc float-field
  [{:keys [onChange]
    :or {onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ field
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ field-wrapper
          ($ NumberInput
             {:ref input
              :className (css ["& input" :border-0])
              & (->
                  props
                  (dissoc :name :className :style)
                  (assoc :onChange
                         (fn [e]
                           (some->>
                             (.. e -target -value)
                             not-empty
                             (re-find #"-?\d+[\.|,]*\d*")
                             (js/parseFloat)
                             onChange))))})))))


(defnc dropdown-field
  [props]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [input area toggle! opened] :as dropdown}
        (use-dropdown
          (->
            props
            (assoc :area-position area-position)
            (dissoc :className)))
        $wrapper (css
                   :flex
                   :justify-between
                   :items-center)
        $decorator (css
                     :text-gray-400
                     ["&.opened" :text-transparent]
                     {:transition "color .2s ease-in-out"})]
    (provider
      {:context dropdown/*dropdown*
       :value dropdown}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
        ($ field
           {:onClick (fn []
                       (toggle!)
                       (when @input (.focus @input)))
            & props}
           ($ field-wrapper
              {:className $wrapper}
              ($ popup/Area
                 {:ref area
                  & (select-keys props [:className])}
                 ($ dropdown/Input {& props})
                 ($ dropdown/Popup
                    {:className "dropdown-popup"
                     :render/option e/dropdown-option
                     :render/wrapper e/dropdown-wrapper}))
              (d/span
                {:class (cond-> [$decorator]
                          opened (conj "opened"))}
                ($ icon/dropdownDecorator))))))))


(defnc multiselect-field
  [{:keys [context-fn search-fn disabled]
    :or {search-fn str}
    :as props}]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        {:keys [remove!
                toggle!
                options
                new-fn
                area]
         :as multiselect} (use-multiselect
                            (assoc props
                                   :search-fn search-fn
                                   :area-position area-position))
        $wrapper (css
                   :flex
                   :items-center
                   {:min-height "3em"})]
    (provider
      {:context dropdown/*dropdown*
       :value multiselect}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
           ($ field
              {:onClick (fn [] (toggle!))
               & props}
              ($ field-wrapper
                 {:className $wrapper}
                 (map
                   (fn [option]
                     ($ e/multiselect-option
                       {:key (search-fn option)
                        :value option
                        :onRemove #(remove! option)
                        :context (if disabled :stale
                                   (when (fn? context-fn)
                                     (context-fn option)))}))
                   (:value props))
                 (when (or (fn? new-fn) (not-empty options))
                   ($ popup/Area
                      {:ref area}
                      ($ dropdown/Input {& props})
                      ($ dropdown/Popup
                         {:className "dropdown-popup"
                          :render/option e/dropdown-option
                          :render/wrapper e/dropdown-wrapper})))))))))


(defnc timestamp-dropdown
  [{:keys [value placeholder disabled className
           read-only onChange format name time?]
    :or {format :full-date
         time? true}}]
  (let [[opened set-opened!] (hooks/use-state false)
        translate (use-translate)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)
        $clear (css
                 :text-gray-400
                 ["&:hover"
                  :text-gray-900
                  {:cursor "pointer"}]
                 {:transition "color .2s ease-in-out"})]
    (popup/use-outside-action
      opened area popup
      (fn [e]
        (when (.contains js/document.body (.-target e))
          (set-opened! false))))
    ($ field
       {:name name
        :onClick (fn []
                   (when (and (not disabled) (not read-only))
                     (set-opened! true)))}
       ($ popup/Area
          {:ref area}
          ($ field-wrapper
             {:className (str className 
                              (when opened " opened")
                              (when disabled " disabled"))}
             ($ AutosizeInput
                {:className "input"
                 :readOnly true
                 :value (when (some? value) (translate value format))
                 :spellCheck false
                 :auto-complete "off"
                 :disabled disabled
                 :placeholder placeholder})
             (when opened
               ($ popup/Element
                  {:ref popup
                   :className className 
                   :wrapper e/dropdown-wrapper
                   :preference popup/cross-preference}
                  ($ e/timestamp-calendar
                     {:value value
                      :disabled disabled
                      :read-only read-only
                      :onChange (fn [x]
                                  (onChange x)
                                  (when-not time? (set-opened! false)))})
                  (when time?
                    ($ e/timestamp-time
                       {:value value
                        :disabled (if-not value true
                                    disabled)
                        :read-only read-only
                        :onChange onChange}))))
             (when value
               (d/span
                 {:class (cond-> [$clear]
                           opened (conj "opened"))
                  :onClick (fn [e]
                             (.stopPropagation e)
                             (.preventDefault e)
                             (onChange nil)
                             (set-opened! false))}
                 ($ icon/clear))))))))


(defnc timestamp-field
  [props]
  ($ timestamp-dropdown
    {:format :medium-datetime & props}))


(defnc date-field
  [props]
  ($ timestamp-dropdown
    {:format :full-date
     :time? false
     & props}))


(defnc period-dropdown
  [{:keys [value placeholder disabled className
           read-only onChange format name time?]
    :or {format :full-date
         time? true}}]
  (let [[start end :as value] (or value [nil nil])
        [opened set-opened!] (hooks/use-state false)
        translate (use-translate)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)
        $clear (css
                 :text-gray-400
                 ["&:hover"
                  :text-gray-900
                  {:cursor "pointer"}]
                 {:transition "color .2s ease-in-out"})]
    (popup/use-outside-action
      opened area popup
      (fn [e]
        (when (.contains js/document.body (.-target e))
          (set-opened! false))))
    ($ field
       {:name name
        :onClick (fn []
                   (when (and (not disabled) (not read-only))
                     (set-opened! true)))}
       ($ popup/Area
          {:ref area}
          ($ field-wrapper
             {:className (str className 
                              (when opened " opened")
                              (when disabled " disabled"))}
             ($ AutosizeInput
                {:className "input"
                 :readOnly true
                 :value (when (or
                                (some? start)
                                (some? end))
                          (cond
                            (and start end)
                            (str (translate start format) " - " (translate end format))
                            ;;
                            (and start (not end))
                            (str
                              (str/capitalize (translate :time.after)) " "
                              (translate start format))
                            ;;
                            (and end (not start))
                            (str
                              (str/capitalize (translate :time.before)) " "
                              (translate end format))))
                 :spellCheck false
                 :auto-complete "off"
                 :disabled disabled
                 :placeholder placeholder})
             (when opened
               ($ popup/Element
                  {:ref popup
                   :className className 
                   :wrapper e/dropdown-wrapper
                   :preference popup/cross-preference}
                  ($ e/period-calendar
                     {:value value
                      :disabled disabled
                      :read-only read-only
                      :onChange onChange})
                  (when time?
                    (d/div
                      {:class (css
                                :flex
                                :justify-around)}
                      ($ e/timestamp-time
                         {:key "start"
                          :value start
                          :disabled (if-not start true
                                      disabled)
                          :read-only read-only
                          :onChange (fn [x]
                                      (onChange (assoc value 0 x)))})
                      ($ e/timestamp-time
                         {:key "end"
                          :value end
                          :disabled (if-not end true
                                      disabled)
                          :read-only read-only
                          :onChange (fn [x]
                                      (onChange (assoc value 1 x)))})))))
             (when value
               (d/span
                 {:class (cond-> [$clear]
                           opened (conj "opened"))
                  :onClick (fn [e]
                             (.stopPropagation e)
                             (.preventDefault e)
                             (onChange nil)
                             (set-opened! false))}
                 ($ icon/clear))))))))


(defnc timestamp-period-field
  [props]
  ($ period-dropdown
    {:format :medium-datetime
     & props}))


(defnc date-period-field
  [props]
  ($ period-dropdown
    {:format :full-date
     :time? false
     & props}))


(defnc checkbox-field
  [{:keys [name] :as props}]
  (let [$default (css
                   :flex
                   :items-center
                   :mx-2
                   :my-3
                   :text-gray-500
                   ["& .field-name"
                    :ml-2
                    :select-none
                    :text-sm
                    :font-bold
                    :uppercase
                    {:transition "all .3s ease-in-out"}])]
    (d/span
      {:class $default}
      ($ ui/checkbox {& (dissoc props :name :className)})
      (d/label
        {:className "field-name"}
        name))))


(defnc identity-dropdown-option
  [{:keys [option] :as props} ref]
  {:wrap [(react/forwardRef)]}
  ($ e/dropdown-option
    {:ref ref
     & (dissoc props :ref :option)}
    ($ e/avatar {:size :small
                 :className (css :mr-2)
                 & option})
    (:name option)))


(defnc identity-field
  [props]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [value input area toggle! opened] :as dropdown}
        (use-dropdown
          (->
            props
            (assoc :area-position area-position
                   :search-fn :name)
            (dissoc :className)))
        $wrapper (css
                   :flex
                   :justify-between
                   :items-center)
        $decorator (css
                     :text-gray-400
                     ["&.opened" :text-transparent]
                     {:transition "color .2s ease-in-out"})]
    (provider
      {:context dropdown/*dropdown*
       :value dropdown}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
        ($ popup/Area
           {:ref area
            & (select-keys props [:className])}
           ($ field
              {:onClick (fn []
                          (toggle!)
                          (when @input (.focus @input)))
               & props}
              ($ field-wrapper
                 {:className $wrapper}
                 ($ e/avatar
                    {:className (css :mr-2
                                     :border
                                     :border-solid
                                     :border-gray-500)
                     :size :small}
                    value)
                 ($ dropdown/Input {& props})
                 (d/span
                   {:class (cond-> [$decorator]
                             opened (conj "opened"))}
                   ($ icon/dropdownDecorator))))
           ($ dropdown/Popup
              {:className "dropdown-popup"
               :render/option identity-dropdown-option
               :render/wrapper e/dropdown-wrapper}))))))


(defnc IdentityMultiselectOption
  [{{:keys [name] :as option} :value :as props}]
  ($ e/multiselect-option
    {& props}
    ($ e/avatar
       {:size :small
        :className (css :mr-2)
        & option})
    (d/div {:className "name"} name)))


(defnc identity-multiselect-field
  [{:keys [context-fn search-fn disabled]
    :or {search-fn str}
    :as props}]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        {:keys [remove!
                toggle!
                options
                new-fn
                area]
         :as multiselect} (use-multiselect
                            (assoc props
                                   :search-fn search-fn
                                   :area-position area-position))
        $wrapper (css
                   :flex
                   :items-center
                   {:min-height "3em"})]
    (provider
      {:context dropdown/*dropdown*
       :value multiselect}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
           ($ field
              {:onClick (fn [] (toggle!))
               & props}
              ($ field-wrapper
                 {:className $wrapper}
                 (map
                   (fn [option]
                     ($ IdentityMultiselectOption
                       {:key (search-fn option)
                        :value option
                        :onRemove #(remove! option)
                        :context (if disabled :stale
                                   (when (fn? context-fn)
                                     (context-fn option)))}))
                   (:value props))
                 (when (or (fn? new-fn) (not-empty options))
                   ($ popup/Area
                      {:ref area}
                      ($ dropdown/Input {& props})
                      ($ dropdown/Popup
                         {:className "dropdown-popup"
                          :render/option identity-dropdown-option
                          :render/wrapper e/dropdown-wrapper})))))))))


(def components
  #:field {:text textarea-field
           :boolean checkbox-field
           :input input-field
           :integer integer-field 
           :float float-field
           :dropdown dropdown-field
           :multiselect multiselect-field
           :timestamp timestamp-field
           :date date-field
           :date-period date-period-field
           :timestamp-period timestamp-period-field
           :identity identity-field
           :identity-multiselect identity-multiselect-field})
