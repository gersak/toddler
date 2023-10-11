(ns toddler.ui.fields
  (:require
    clojure.set
    [clojure.string :as str]
    [clojure.core.async :as async]
    [shadow.css :refer [css]]
    [goog.string.format]
    [helix.core
     :refer [$ defnc provider]]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.hooks  :as hooks]
    [toddler.input
     :as input
     :refer [TextAreaElement]]
    [toddler.i18n :as i18n]
    [toddler.hooks :refer [use-translate]]
    [toddler.ui.elements :as e]
    [toddler.dropdown :as dropdown
     :refer [use-dropdown]]
    [toddler.multiselect
     :refer [use-multiselect]]
    [toddler.ui :as ui]
    [toddler.popup :as popup]))


(defnc field
  [{:keys [name className style]
    :as props} _ref]
  {:wrap [(ui/forward-ref)]}
  (let [$style (css
                 :flex :flex-col
                 :mx-2 :my-1 :grow
                 {:border-bottom "1px solid transparent"
                  :transition "all .3s ease-in-out"}
                 ["& .field-name"
                  :text-xs
                  :font-bold
                  :uppercase
                  {:color "#235568"
                   :user-select "none"
                   :transition "all .3s ease-in-out"}]
                 ["&.empty:hover" :border-neutral-400]
                 ["&:hover .field-name,&:focus-within .field-name" {:text-shadow "0px 0px 11px #01282f73"
                                                                    :color "#235568"}]
                 ["& input,& textarea" :text-neutral-600 :border-neutral-600 {:flex-grow "1"}]
                 ["&.empty" :border-neutral-300 {:border-bottom "1px solid"}])]
    (d/div
      {:ref _ref
       :class (cond-> [className $style]
                (:empty? props) (conj "empty"))
       :style style 
       & (select-keys props [:onClick])}
      (when name (d/label {:className "field-name"} name))
      (c/children props))))



(defnc field-wrapper
  [{:keys [className] :as props}]
  (let [$style (css
                 :flex
                 :items-center
                 :rounded-md
                 :cursor-text
                 :grow
                 {:transition "all .3s ease-in-out"})]
  (d/div
    {:class [className
             $style]}
    (c/children props))))


(defnc textarea-field
  [props]
  (let [$style (css
                 ["& textarea" {:font-family "Roboto"}])
        _input (hooks/use-ref nil)]
    ($ field
       {:className $style
        :onClick (fn [] (when @_input (.focus @_input)))
        & props}
       ($ field-wrapper
          {:className (css
                        :grow
                        ["& textarea"
                         {:overflow "hidden"
                          ; :min-height "2em"
                          :border "none"
                          :resize "none"
                          :box-sizing "border-box"
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
          (d/input
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



(letfn [(text->number [text]
          (if (empty? text) nil
            (let [number (js/parseInt text)]
              (when-not (js/Number.isNaN number) number))))]
  (defnc integer-field
    [{:keys [onChange value read-only disabled]
      :as props}]
    (let [translate (use-translate)
          [input set-input!] (hooks/use-state (when value (translate value)))
          [focused? set-focused!] (hooks/use-state false)
          _ref (hooks/use-ref nil)
          $float (css
                   :border-0
                   :outline-0
                   :text-sm)]
      (hooks/use-effect
        [focused?]
        (set-input! (str value)))
      (hooks/use-effect
        [value]
        (when-not (= (text->number input) value)
          (set-input! (when value (translate value)))))
      ($ field
         {:onClick (fn [] (when @_ref (.focus @_ref)))
          :empty? (nil? value)
          & props}
         ($ field-wrapper
            (d/input
              {:ref _ref
               :className $float
               :value (if value
                        (if focused? 
                          input
                          (translate value))
                        "")
               :read-only read-only
               :disabled disabled
               :onFocus #(set-focused! true)
               :onBlur #(set-focused! false)
               :onChange (fn [e]
                           (let [text (as-> (.. e -target -value) t
                                        (re-find #"\d*" t))
                                 number (js/parseInt text)]
                             (if (empty? text) (onChange nil)
                               (when-not (js/Number.isNaN number)
                                 (set-input! text)
                                 (when (fn? onChange) (onChange number))))))}))))))



(letfn [(text->number [text]
          (if (empty? text) nil
            (let [number (js/parseFloat text)]
              (when-not (js/Number.isNaN number) number))))]
  (defnc float-field
    [{:keys [onChange value read-only disabled]
      :as props}]
    (let [translate (use-translate)
          [input set-input!] (hooks/use-state (when value (translate value)))
          [focused? set-focused!] (hooks/use-state false)
          _ref (hooks/use-ref nil)
          $float (css
                   :border-0
                   :outline-0
                   :text-sm)]
      (hooks/use-effect
        [focused?]
        (set-input! (str value)))
      (hooks/use-effect
        [value]
        (when-not (= (text->number input) value)
          (set-input! (when value (translate value)))))
      ($ field
         {:onClick (fn [] (when @_ref (.focus @_ref)))
          :empty? (nil? value)
          & props}
         ($ field-wrapper
            (d/input
              {:ref _ref
               :className $float
               :value (if value
                        (if focused? 
                          input
                          (translate value))
                        "")
               :read-only read-only
               :disabled disabled
               :onFocus #(set-focused! true)
               :onBlur #(set-focused! false)
               :onChange (fn [e]
                           (let [text (as-> (.. e -target -value) t
                                        (re-find #"[\d\.,]*" t)
                                        (str/replace t #"\.(?=[^.]*\.)" "")
                                        (str/replace t #"[\.\,]+" "."))
                                 number (js/parseFloat text)]
                             (if (empty? text) (onChange nil)
                               (when-not (js/Number.isNaN number)
                                 (set-input! text)
                                 (when (fn? onChange) (onChange number))))))}))))))


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
                     :text-neutral-400
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
            :empty? (nil? (:value props))
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
                #_($ icon/dropdownDecorator))))))))


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
               :empty? (empty? (:value props))
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


(def $clear
  (css
    ["& .clear" :text-transparent :cursor-pointer
     {:transition "color .3s ease-in-out"}]
    ["&:hover .clear" :text-neutral-400]
    ["& .clear:hover" :text-neutral-600]))


(defnc timestamp-dropdown
  [{:keys [value disabled className
           read-only onChange format name time?]
    :or {format :full-date
         time? true}}]
  (let [[opened set-opened!] (hooks/use-state false)
        translate (use-translate)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (popup/use-outside-action
      opened area popup
      (fn [e]
        (when (.contains js/document.body (.-target e))
          (set-opened! false))))
    ($ field
       {:name name
        :className (str/join " " [className $clear])
        :empty? (nil? value)
        :onClick (fn []
                   (when (and (not disabled) (not read-only))
                     (set-opened! true)))}
       ($ popup/Area
          {:ref area}
          ($ field-wrapper
             {:className (str className 
                              (when opened " opened")
                              (when disabled " disabled"))}
             (d/input
                {:className "input"
                 :readOnly true
                 :value (if-not (some? value) ""
                          (translate value format))
                 :spellCheck false
                 :auto-complete "off"
                 :disabled disabled})
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
                 {:class (cond-> ["clear"]
                           opened (conj "opened"))
                  :onClick (fn [e]
                             (.stopPropagation e)
                             (.preventDefault e)
                             (onChange nil)
                             (set-opened! false))}
                 #_($ icon/clear))))))))


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
  [{:keys [value disabled className
           read-only onChange format name time?]
    :or {format :full-date
         time? true}}]
  (let [[start end :as value] (or value [nil nil])
        [opened set-opened!] (hooks/use-state false)
        translate (use-translate)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (popup/use-outside-action
      opened area popup
      (fn [e]
        (when (.contains js/document.body (.-target e))
          (set-opened! false))))
    ($ field
       {:name name
        :className (str/join " " [className $clear])
        :empty? (every? nil? value)
        :onClick (fn []
                   (when (and (not disabled) (not read-only))
                     (set-opened! true)))}
       ($ popup/Area
          {:ref area}
          ($ field-wrapper
             {:className (str className 
                              (when opened " opened")
                              (when disabled " disabled"))}
             (d/input
                {:className "input"
                 :readOnly true
                 :value (if-not (or
                                  (some? start)
                                  (some? end))
                          ""
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
                 :disabled disabled})
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
             (when (some some? value)
               (d/span
                 {:class (cond-> ["clear"]
                           opened (conj "opened"))
                  :onClick (fn [e]
                             (.stopPropagation e)
                             (.preventDefault e)
                             (onChange nil)
                             (set-opened! false))}
                 #_($ icon/clear))))))))


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
                   :text-neutral-500
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


(defnc checklist-field
  [props]
  (let [$style (css
                 ["& .row"
                  :flex
                  :items-center
                  :cursor-pointer
                  :mx-2
                  :my-3
                  :text-neutral-600]
                 ["& .icon"
                  :cursor-pointer
                  :text-neutral-300
                  :w-4
                  :h-4
                  :flex
                  :justify-center
                  :items-center
                  :outline-none]
                 ["& path" :cursor-pointer]
                 ["&:active" :border-transparent]
                 ["& .row.selected .icon" :text-neutral-600]
                 ["& .row.disabled" :pointer-events-none]
                 ["& .row .name"
                   :ml-2
                   :select-none
                   :text-sm
                   :font-bold
                   :uppercase
                   {:transition "all .3s ease-in-out"}]
                 ["& .row.selected .name"
                   :ml-2
                   :select-none
                   :text-sm
                   :font-bold
                   :uppercase
                   {:transition "all .3s ease-in-out"}])]
    ($ field
       {& props}
       ($ e/checklist
          {:className $style
           & (dissoc props :className)}))))



(defnc identity-field
  [{:keys [className] :as props}]
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
                   :items-center
                   :h-8)
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
            :empty? (nil? (:value props))
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
              ($ popup/Area
                 {:ref area
                  :className (str/join " " [className (css :grow
                                                           :flex
                                                           :items-center)])}
                 ($ dropdown/Popup
                    {:className "dropdown-popup"
                     :render/option e/identity-dropdown-option
                     :render/wrapper e/dropdown-wrapper})
                 ($ dropdown/Input
                    {:className (css :flex :grow) & props})
                 (d/span
                   {:class (cond-> [$decorator]
                             opened (conj "opened"))}
                   #_($ icon/dropdownDecorator)))))))))


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
               :empty? (every? nil? (:value props))
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
                          :render/option e/identity-dropdown-option
                          :render/wrapper e/dropdown-wrapper})))))))))


(defnc currency-field
  [{:keys [disabled onChange value name
           onFocus on-focus className
           read-only]
    :or {onChange identity}}]
  (let [{:keys [currency amount]} value
        ;;
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [input area toggle! close!] :as dropdown}
        (use-dropdown
          (->
            (hash-map
              :value currency
              :area-position area-position
              :options ["EUR" "USD" "JPY" "AUD" "CAD" "CHF"]
              :onChange (fn [x] (onChange {:amount amount :currency x})))
            (dissoc :className)))
        on-focus (or onFocus on-focus identity)
        [focused? set-focused!] (hooks/use-state nil)
        [state set-state!] (hooks/use-state (when amount (i18n/translate amount value)))
        amount' (if (some? amount)
                 (if (not focused?)
                   (when amount (i18n/translate amount currency))
                   (str state))
                 "")
        field-ref (hooks/use-ref nil)]
      (hooks/use-effect
        [focused?]
        (when focused?
          (letfn [(check-focus [e]
                    (when (and
                            (some? @field-ref)
                            (not (.contains @field-ref (.-target e))))
                      (set-focused! false)))]
            (.addEventListener js/document "click" check-focus)
            (fn []
              (.removeEventListener js/document "click" check-focus)))))
      (hooks/use-effect
        [currency]
        (async/go
          (async/<! (async/timeout 300))
          (.focus @input)))
    ($ field
       {:ref #(reset! field-ref %)
        :name name
        :className (str/join " " [className $clear])
        :empty? (nil? amount)
        :onClick (fn [] (toggle!))}
       ($ field-wrapper
          (provider
            {:context dropdown/*dropdown*
             :value dropdown}
            (provider
              {:context popup/*area-position*
               :value [area-position set-area-position!]}
              ($ popup/Area
                 {:ref area
                  :className (css :flex :items-center)}
                 (d/input
                   {:value (or currency "")
                    :disabled true
                    :className (css
                                 :w-8
                                 :font-bold
                                 :text-sm
                                 :cursor-pointer)
                    :read-only true})
                 ($ dropdown/Popup
                    {:render/option e/dropdown-option
                     :render/wrapper e/dropdown-wrapper}))))
          (d/input
            {:ref input
             :value amount'
             :read-only read-only
             :disabled (or disabled (not currency))
             :onFocus (fn [e] (set-focused! true) (on-focus e))
             :onKeyDown (fn [e]
                          (case (.-keyCode e)
                            ;; ESCAPE or ENTER
                            (13 27) (do
                                      (.blur @input)
                                      (close!)
                                      (set-focused! false))
                            ;; TAB
                            9 (do
                                (close!)
                                (set-focused! false))
                            ;; EVERYTHING ELSE
                            "default"))
             :onChange (fn [e]
                         (when (some? currency)
                           (let [text (as-> (.. e -target -value) t
                                        (re-find #"[\d\.,]*" t)
                                        (str/replace t #"\.(?=[^.]*\.)" "")
                                        (str/replace t #"[\.\,]+" "."))
                                 number (js/parseFloat text)]
                             (if (empty? text) (onChange {:amount nil
                                                          :currency currency})
                               (when-not (js/Number.isNaN number)
                                 (set-state! text)
                                 (when (fn? onChange)
                                   (onChange
                                     {:amount number
                                      :currency currency})))))))})
          (when value
            (d/span
              {:class ["clear"]
               :onClick (fn [e]
                          (.stopPropagation e)
                          (.preventDefault e)
                          (onChange nil))}
              #_($ icon/clear)))))))


(def components
  #:field {:text textarea-field
           :boolean checkbox-field
           :checklist checklist-field
           :input input-field
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
