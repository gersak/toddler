(ns toddler.ui.default.fields
  (:require
    clojure.set
    clojure.string
    [goog.string.format]
    [cljs-bean.core :refer [->clj]]
    [helix.styled-components :refer [defstyled]]
    [helix.core
     :refer [$ defnc]]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.hooks  :as hooks]
    [toddler.elements.input
     :refer [AutosizeInput
             NumberInput
             TextAreaElement]]
    [toddler.ui.default.color :refer [color]]
    [toddler.ui.default.elements :as e]
    [toddler.elements.dropdown :as dropdown]
    [toddler.elements.date :as date]
    [toddler.ui :as ui]
    [toddler.ui.provider :refer [UI ExtendUI]]
    ["react" :as react]))


(defnc Field
  [{:keys [name className style]
    :as props}]
  (d/div
   {:class className
    :style (->clj style)
    & (select-keys props [:onClick])}
   (when name (d/label {:className "field-name"} name))
   (c/children props)))


(defstyled default-field Field
  {:display "flex"
   :flex-direction "column"
   :margin "5px 10px"
   ".field-name"
   {:color (color :gray)
    :user-select "none"
    :transition "all .3s ease-in-out"
    :font-size 12
    :font-weight "600"
    :text-transform "uppercase"}})


(defstyled field-wrapper
  "div"
  {:border "1px solid"
   :border-radius 2
   :margin-top 4
   :padding "4px 10px"
   :cursor "text"
   :input {:font-size "12"}
   :overflow "hidden"
   :border-color "#b3b3b3 !important"
   :background "#e5e5e5"
   :transition "all .3s ease-in-out"
   ":focus-within" {:border-color (str (color :teal) "!important")
                    :box-shadow (str "0 0 3px " (color :teal))
                    :background-color "transparent"}
   "input,textarea"
   {:color (color :gray)}})


(defstyled text-area-wrapper field-wrapper
  {:flex-grow "1"
   :textarea
   {:overflow "hidden"
    :border "none"
    :resize "none"
    :font-size "12"}})


(defnc TextareaField
  [{:keys [style]
    :as props} _ref]
  {:wrap [(react/forwardRef)]}
  ($ default-field {& props}
    ($ text-area-wrapper
       ($ TextAreaElement
          {:spellCheck false
           :auto-complete "off"
           :style style
           :className "input"
           & (cond->
               (->
                 props
                 (dissoc :name :style :className)
                 (update :value #(or % "")))
               _ref (assoc :ref _ref))}))))

(defstyled textarea-field TextareaField
  {:textarea {:font-family "Roboto"}})


(defnc WrappedField
  [{:keys [context]
    :as props}]
  ($ default-field {& props}
    ($ field-wrapper {:context context}
       (c/children props))))


(defstyled autosize-input AutosizeInput
  {:outline "none"
   :border "none"})


(defnc InputField
  [{:keys [onChange on-change] :as props}]
  (let [_input (hooks/use-ref nil)
        onChange (hooks/use-memo
                   [onChange on-change]
                   (or onChange on-change identity))]
    ($ WrappedField
       {:onClick (fn [] (when @_input (.focus @_input)))
        & props}
       ($ autosize-input
          {:ref _input
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           :onChange (fn [^js e]
                       (onChange (.. e -target -value)))
           & (dissoc props :onChange :name :className :style)}))))


(defnc IntegerField
  [{:keys [onChange]
    :or {onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ WrappedField
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ NumberInput
          {:ref input
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
                        onChange))))}))))


(defstyled integer-field IntegerField {:input {:border "none"}})


(defnc FloatField
  [{:keys [onChange]
    :or {onChange identity}
    :as props}]
  (let [input (hooks/use-ref nil)]
    ($ WrappedField
       {:onClick (fn [] (when @input (.focus @input)))
        & props}
       ($ NumberInput
          {:ref input
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
                        onChange))))}))))


(defstyled float-field FloatField {:input {:border "none"}})


(defstyled dropdown-field-wrapper field-wrapper
  {:position "relative"
   :padding "4px 16px 4px 4px"
   :cursor "pointer"})


(defstyled dropdown-element-decorator dropdown/Decorator
  {:position "absolute"
   :right 4 
   :top 7
   :transition "color .2s ease-in-out"
   :color (color :gray)
   "&.opened" {:color "transparent"}})

(defnc DropdownInput
  [props]
  ($ ExtendUI
    {:components {:input autosize-input
                  :wrapper dropdown-field-wrapper}}
    ($ dropdown/Input
       {& props}
       ($ dropdown-element-decorator {:className "decorator"}))))


(defnc DropdownField
  [props]
  ($ default-field {& props}
     ($ ExtendUI
        {:components {:input DropdownInput
                      :popup e/DropdownPopup}}
        ($ dropdown/Element
           {:className "dropdown"
            & (dissoc props :name :className :style)}))))


(defstyled multiselect-field-wrapper field-wrapper
  {:position "relative"
   :padding "4px 16px 4px 4px"
   :border "1px solid black"
   :min-width 100
   ".tags" {:display "flex"
            :flex-direction "row"
            :flex-wrap "wrap"
            :align-items "baseline"
            (str autosize-input) {:align-self "center"}}})


(defnc MultiselectField
  [props]
  ($ UI
    {:components {:wrapper multiselect-field-wrapper
                  :input e/MultiselectInput
                  :popup e/DropdownPopup
                  :option e/multiselect-option}}
    ($ WrappedField
       {& props}
       ($ e/multiselect
          {& (dissoc props :name :className :style)}))))


(defstyled multiselect-field MultiselectField
  {".multiselect"
   {:display "flex"
    :align-items "center"
    :flex-wrap "wrap"
    :min-height 30}})


(defnc TimestampFieldInput
  [props]
  ($ ExtendUI
    {:components
     {:wrapper dropdown-field-wrapper}}
    ($ date/TimestampInput {& props})))


(defnc TimestampField
  [{:keys [value placeholder disabled
           read-only onChange format]
    :or {format :datetime-full}
    :as props}]
  ($ default-field
    {& props}
    ($ ExtendUI
       {:components
        {:popup e/TimestampPopup
         :field TimestampFieldInput}}
       ($ date/TimestampDropdown
          {:value value
           :onChange onChange
           :placeholder placeholder
           :disabled disabled
           :read-only read-only
           :format format
           :className "data"}))))


(defnc PeriodFieldInput
  [props] 
  ($ ExtendUI
    {:components {:wrapper dropdown-field-wrapper}}
    ($ date/PeriodInput
       {& props})))


(defnc PeriodField
  [{:keys [value placeholder disabled
           read-only onChange format]
    :or {format :medium-datetime}
    :as props}]
  ($ date/PeriodElementProvider
    {& props}
    ($ default-field
       {& props}
       ($ ExtendUI
          {:components
           {:popup e/period-popup
            :field PeriodFieldInput}}
          ($ date/PeriodDropdown
             {:value value
              :onChange onChange
              :placeholder placeholder
              :disabled disabled
              :read-only read-only
              :format format
              :className "data"})))))


(defnc CheckboxField [{:keys [name className] :as props}]
  (d/span
    {:class className}
    ($ ui/checkbox {& (dissoc props :name :className)})
    (d/label
      {:className "field-name"}
      name)))


(defstyled checkbox-field CheckboxField
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :margin "5px 10px"
   ".field-name"
   {:margin-left 5
    :user-select "none"
    :transition "all .3s ease-in-out"
    :font-size "12"
    :font-weight "600"
    :text-transform "uppercase"}
   :color (color :gray)})


(defstyled field-avatar e/small-avatar
  {:margin-right 8})


(defnc IdentityFieldInput
  [props]
  ($ UI
    {:components
     {:img field-avatar
      :input autosize-input
      :wrapper dropdown-field-wrapper}}
    ($ dropdown/Input
       {& props})))


(defstyled identity-field-input IdentityFieldInput
  {:display "flex"
   :align-items "center"})


(defnc IdentityElement
  [props]
  ($ ExtendUI
    {:components
     {:popup e/identity-popup
      :input identity-field-input}}
    ($ dropdown/Element
       {:search-fn :name
        & (dissoc props :search-fn)})))


(defnc IdentityField
  [props]
  ($ default-field
     {& props}
     ($ IdentityElement
        {:className "input"
         & (->
             props
             (dissoc :name :style :className)
             (update :value #(or % "")))})))


(defnc IdentityMultiselectElement
  [props]
  ($ ExtendUI
    {:components
     {:popup e/identity-popup
      :input identity-field-input}}
    (let [search-fn :name
          display-fn (fn [option] ($ ui/avatar {& option}))]
      ($ e/multiselect
         {:search-fn search-fn
          :display-fn display-fn
          :className "multiselect"
          & (dissoc props :name :className :style)}))))


(defnc IdentityMultiselectOption
  [{{:keys [name] :as option} :value :as props}]
  ($ e/multiselect-option
    {:& props}
    ($ field-avatar {& option})
    (d/div {:className "name"} name)))


(defnc IdentityMultiselectInput
  [props]
  ($ ExtendUI
    {:components
     {:img field-avatar
      :input autosize-input
      :wrapper e/multiselect-wrapper}}
    ($ dropdown/Input
       {& props})))


(defnc IdentityMultiselectField
  [props]
  ($ ExtendUI
    {:components {:wrapper multiselect-field-wrapper
                  :input IdentityMultiselectInput
                  :popup e/identity-popup
                  :option IdentityMultiselectOption}}
    ($ WrappedField
       {& props}
       ($ e/multiselect
          {& (dissoc props :name :className :style)}))))



(def components
  #:field {:text textarea-field
           :boolean checkbox-field
           :input InputField
           :integer integer-field 
           :float float-field
           :dropdown DropdownField
           :multiselect multiselect-field
           :timestamp TimestampField
           :period PeriodField
           :identity IdentityField
           :identity-multiselect IdentityMultiselectField})
