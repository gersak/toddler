(ns toddler.elements
  (:require
    clojure.set
    clojure.string
    ; [clojure.data :refer [diff]]
    ; [clojure.core.async :as async]
    [goog.string.format]
    [vura.core :as vura]
    [cljs-bean.core :refer [->js ->clj]]
    [helix.styled-components :refer [defstyled --themed]]
    [helix.core
     :refer [$ defnc fnc provider
             defhook create-context memo]]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.hooks  :as hooks]
    [helix.spring :as spring]
    [toddler.app :as app]
    [toddler.hooks
     :refer [#_make-idle-service
             use-delayed
             use-dimensions
             use-idle]]
    [toddler.input
     :refer [AutosizeInput
             NumberInput
             IdleInput
             TextAreaElement
             SliderElement]]
    [toddler.mask :refer [use-mask]]
    [toddler.popup :as popup]
    [toddler.tooltip :as tip]
    [toddler.ui :as ui]
    ["react" :as react]
    ["toddler-icons$default" :as icon]))


(defn --flex-position
  [{:keys [position]}]
  (when-some [jc (case position
                   :center "center"
                   :end "flex-end"
                   :explode "space-between"
                   nil)]
    {:justify-content jc
     ".wrapper" {:justify-content jc}}))


(def ^:dynamic ^js *container* (create-context nil))
(def ^:dynamic ^js *container-dimensions* (create-context nil))
(def ^:dynamic ^js *container-style* (create-context nil))


(defhook use-layout
  ([] (hooks/use-context app/*layout*))
  ([k] (get (hooks/use-context app/*layout*) k)))

(defhook use-window [] (hooks/use-context app/*window*))

(defhook use-container [] (hooks/use-context *container*))
(defhook use-container-dimensions [] (hooks/use-context *container-dimensions*))


(letfn [(same? [a b]
          (let [ks [:style :className]
                before (select-keys a ks)
                after (select-keys b ks)
                result (= before after)]
            result))]
  (defnc Container
    [{:keys [className style] :as props}]
    {:wrap [(memo same?)]}
    (let [[container dimensions] (use-dimensions)]
      (d/div
        {:ref #(reset! container %)
         :className className
         :style style}
        (provider
          {:context *container-dimensions*
           :value dimensions}
          (provider
            {:context *container*
             :value container}
            (c/children props)))))))


(defnc Column
  [{:keys [label style className position] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (d/div
    {:ref _ref
     :className className
     :style style 
     :position position}
    (when label
      (d/div
        {:className "label"}
        (d/label label)))
    (c/children props)))


(defnc Row
  [{:keys [label className style position] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (d/div
    {:ref _ref
     :className className
     :style style 
     :position position}
    (when label
      (d/div
        {:className "label"}
        (d/label label)))
    (c/children props)))

;;

(defnc Info
  [{:keys [text] :as props}]
  (d/div
   {& (dissoc props :text)}
   ($ icon/info)
   (d/p text)))


; (defstyled info Info
;   {:display "flex"
;    :align-items "baseline"
;    :p {:margin "5px 0"}})


(def action-tooltip tip/action-tooltip)


(defnc Tooltip
  [{:keys [message preference className disabled] 
    :or {preference popup/cross-preference}
    :as props} ref]
  {:wrap [(react/forwardRef)]}
  (let [[visible? set-visible!] (hooks/use-state nil)
        hidden? (use-delayed (not visible?) 300)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (if (and (some? message) (not disabled)) 
      ($ popup/Area
         {:ref area
          :className "popup_area"
          :onMouseLeave (fn [] (set-visible! false))
          :onMouseEnter (fn [] (set-visible! true))}
         (c/children props)
         (when visible?
           ($ popup/Element
              {:ref (or ref popup)
               :style {:visibility (if hidden? "hidden" "visible")
                       :animationDuration ".5s"
                       :animationDelay ".3s"}
               :preference preference
               :className (str className " animated fadeIn")}
              (d/div {:class "info-tooltip"} message))))
      (c/children props))))



(defnc Action
  [{:keys [tooltip disabled]
    icon :icon
    :as props}]
  (let [[style api] (spring/use-spring (fn [] {:transform "scale(1)"}))]
    ($ action-tooltip
       {:message tooltip
        :disabled (or (empty? tooltip) disabled)}
       (spring/div
         {:style style
          :onMouseDown (fn [] (api :start {:transform "scale(0.6)" :config {:tension 2000}}))
          :onMouseUp (fn [] (api :start {:transform "scale(1)" :config {:delay 200 :tension 2000}}))
          :onMouseEnter #(api :start {:transform "scale(1.2)" :config {:tension 2000}})
          :onMouseLeave #(api :start {:transform "scale(1)" :config {:tension 2000}})
          :& (dissoc props :tooltip :icon :icon-position)}
         (when icon ($ icon))
         (c/children props)))))


(def $action
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :padding 3
   :use-select "none"
   :cursor :pointer
   :font-size "1em"
   :transition "all .2s linear"
   :path {:cursor "pointer"}
   :user-select "none"
   :margin "0px 5px"
   :min-height 36
   :svg {:margin "0 3px"}
   ":hover"
   {:opacity "1"}})


(defstyled action Action
  $action
  --themed)


(defstyled named-action
  "div"
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :padding 3
   :use-select "none"
   :cursor :pointer
   :opacity ".7"
   :font-size "1.2em"
   :transition "all .2s linear"
   :path {:cursor "pointer"}
   :user-select "none"
   :margin "7px 4px"
   :svg {:margin "0 3px"}
   ":hover"
   {:opacity "1"
    :font-size "1.4em"}}
  (fn [{:keys [icon-position]}]
    (case icon-position
      :right {:svg {:margin-left 5}}
      {:svg {:margin-right 5}}))
  --themed)


(defnc Checkbox [{:keys [value className] :as props}]
  (d/button
    {:className className
     :value value & (dissoc props :active)}
    ($ (case value
         nil icon/checkboxDefault
         icon/checkbox))))


(defstyled interactions
  "div"
  {:display "flex"
   :flex-direction "row"
   :flex-wrap "wrap"
   :align-items "center"
   :padding "0 5px;"
   :min-height 40
   (str " " action)
   {:padding "3px"
    :margin "7px 2px"}}
  --themed
  --flex-position)


(defn --editable-tag [{:keys [editable?]}]
  (when-not editable?
    {:user-select "none"}))


(def $tag
  {:margin 3
   :display "flex"
   :flex-direction "row"
   :justify-content "start"
   :align-items "center"
   ; :flex-wrap "wrap"
   ".content"
   {:padding "5px 5px"
    :justify-content "center"
    :align-items "center"
    :font-size "1em"
    :display "flex"}
   :svg {:margin "0 5px"
         :padding-right 3}
   :border-radius 3})


(defnc DefaultTagContent
  [{:keys [value className]}]
  (d/div {:className className} value))


(defnc Tag
  [{:keys [value
           context
           on-remove
           onRemove
           disabled
           className
           render/content]
    :or {content DefaultTagContent}}]
  (let [on-remove (some #(when (fn? %) %) [onRemove on-remove])]
    (d/div
     {:context (if disabled :stale context)
      :className className}
     ($ content {:className "content" :value value})
     (when on-remove
       ($ icon/clear
          {:className "remove"
           :onClick (fn [e]
                      (.stopPropagation e)
                      (on-remove value))})))))


(defstyled tag Tag
  $tag
  --themed)


(defstyled slider SliderElement
  {:-webkit-appearance "none"
   :appearance "none"
   :outline "none"
   :opacity "0.7"
   :padding 2
   :transition "opacity .2s"}
  --themed)


(defnc Mask [props]
  (let [props' (use-mask props)]
    (d/div
     {:class "mask"}
     (d/input
      {:spellCheck false
       :auto-complete "off"
       & (dissoc props' :constraints :delimiters :mask)}))))


(defstyled mask-input Mask
  {:outline "none"
   :border "none"})


;; TODO  move to fields
(defnc CurrencyElement
  [{:keys [currency amount
           currency/options placeholder
           className onChange
           onBlur on-blur onFocus on-focus]}]
  (let [on-blur (or onBlur on-blur identity)
        on-focus (or onFocus on-focus identity)
        [focused? set-focused!] (hooks/use-state nil)
        value (if (some? amount)
                (if (not focused?)
                  (str amount)
                  ; (format-currency currency amount)
                  (str amount))
                "")]
    (d/div
     {:className className}
     #_($ DropdownElement
        {:options options
         :value currency
         :placeholder "VAL"
         :onChange (fn [currency]
                     (onChange {:currency currency
                                :amount amount}))
         :className "dropdown"})
     (d/input
      {:value value
       :autoComplete "off"
       :autoCorrect "off"
       :spellCheck "false"
       :autoCapitalize "false"
       :placeholder placeholder
       :disabled (nil? amount)
       :onChange (fn [e]
                   (some->>
                    (.. e -target -value)
                    not-empty
                    (re-find #"-?\d+[\.|,]*\d*")
                    (js/parseFloat)
                    onChange))
       :className "input"
       :onBlur (fn [e]
                 (set-focused! false)
                 (on-blur e))
         ;;
       :onFocus (fn [e]
                  (set-focused! true)
                  (on-focus e))}))))


(defnc Search
  [{:keys [value icon on-change idle-timeout className onChange]
    :or {idle-timeout 500
         value ""}
    :as props}]
  (let [on-change (or on-change onChange identity)
        [input set-input!] (use-idle "" #(on-change %) idle-timeout)]
    (hooks/use-effect
     [value]
     (when (not= value input)
       (set-input! value)))
    (d/div
     {:className className}
     (d/div
      {:class "value"}
      ($ AutosizeInput
         {& (merge
             (dissoc props :className)
             {:value input
              :on-change (fn [e] (set-input! (.. e -target -value)))})}))
     (when icon ($ icon)))))


(defstyled search Search
  {:display "flex"
   :align-items "center"
   :padding "3px 5px"
   :border "1px solid"
   :border-radius 5
   ".value" {:order 2
             :input {:min-width 250}}
   ".icon" {:order 1 :margin-right "5px"}
   :input {:outline "none" :border "none"}}
  --themed)


(defnc CardAction
  [{:keys [className onClick tooltip disabled]
    _icon :icon}]
  ($ ui/tooltip
     {:message tooltip
      :disabled (or (empty? tooltip) disabled)}
     (d/div
      {:className className}
      (d/div
       {:className "action"
        :onClick onClick}
       ($ _icon)))))


(defnc CardActions
  [{:keys [className] :as props}]
  (d/div
   {:className className}
   (d/div
    {:className "wrapper"}
    (c/children props))))


(defnc ChecklistField 
  [{cname :name 
    value :value 
    onChange :onChange}]
  (d/div
    {:class "row"}
    (d/div 
      {:class "value"
       :onClick #(onChange (not value))}
      ($ (case value
           true icon/checklistSelected
           icon/checklistEmpty)
         {:className "icon"}))
    (d/div 
      {:class "name"}
      cname)))


(defnc ChecklistElement [{:keys [value
                                 options
                                 multiselect?
                                 display-fn
                                 onChange
                                 className] 
                          :or {display-fn identity
                               onChange identity
                               value []}}]
  (let [value' (clojure.set/intersection
                 (set options)
                 (if multiselect? 
                   (set value)
                   #{value}))] 
    (d/div
      {:className className}
      (d/div
        {:class "list"}
        (map
          (fn [option]
            ($ ChecklistField
              {:key (display-fn option)
               :name (display-fn option)
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

(defstyled checklist ChecklistElement
  {:display "flex"
   :justify-content "center"
   ".list" 
   {:display "flex"
    :flex-direction "column"
    :flex-wrap "wrap"
    ".row" 
    {:display "flex"
     :align-content "center"
     :margin-bottom 3
     :max-width 250
     ".value" {:display "flex" 
               :justify-content "center" 
               :align-items "center"
               ".icon" {:cursor "pointer"}}}}})



(defn get-window-dimensions
  []
  (let [w (vura/round-number (..  js/window -visualViewport -width) 1 :floor)
        h (vura/round-number (.. js/window -visualViewport -height) 1 :floor)]
    {:x 0
     :y 0
     :top 0
     :bottom h
     :left 0
     :right w
     :width w
     :height h}))


(def ^:dynamic *window-resizing* (create-context))


(defn wrap-container
  ([component]
   (fnc Container [props]
     ($ Container ($ component {& props}))))
  ([component cprops]
   (fnc [props]
     ($ Container {& cprops} ($ component {& props})))))

;;


;; Could be usefool

; (defn inject-handler [props evnts f]
;   (reduce
;     (fn [props' evt]
;       (if-let [oh (get props' evt)]
;         (if (fn? oh)
;           (assoc props' evt (fn [& args] (apply f args) (apply oh args)))
;           props')
;         props'))
;     props
;     evnts))

; (defn append-handler [props evnts f]
;   (reduce
;     (fn [props' evt]
;       (if-let [oh (get props' evt)]
;         (if (fn? oh)
;           (assoc props' evt (fn [& args] (apply oh args) (apply f args)))
;           props')
;         props'))
;     props
;     evnts))
