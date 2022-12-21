(ns toddler.input
  (:require
   clojure.string
   clojure.edn
   ["react" :as react]
   [cljs.core.async :as async]
   [helix.core :refer [defnc $ <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.util :as util]
   [toddler.mask :refer [use-mask]]
   [toddler.hooks :refer [use-translate use-idle use-delayed]]))


(defnc IdleInput
  [{:keys [value
           timeout
           onChange]
    :or {timeout 500}
    :as props}
   _ref]
  {:wrap [(react/forwardRef)]}
  (let [[_value set-value!] (hooks/use-state (or value ""))
        input (hooks/use-ref nil)
        idle-channel (hooks/use-ref nil)]
    (hooks/use-effect
     [value]
     (set-value! value))
    (hooks/use-effect
     [onChange]
     (when (fn? onChange)
       (reset!
        idle-channel
        (util/make-idle-service
         timeout
         (fn [values]
           (onChange (not-empty (last (butlast values))))))))
     (fn []
       (when @idle-channel
         (async/close! @idle-channel))))
    (d/input
     {:value (or _value "")
      :ref #(reset! (or _ref input) %)
      :autoComplete "off"
      :autoCorrect "off"
      :spellCheck "false"
      :autoCapitalize "false"
      :onChange (fn [e]
                  (when-let [v (.. e -target -value)]
                    (when @idle-channel
                      (async/put! @idle-channel v))
                    (set-value! v)))
      & (dissoc props :on-change :onChange :timeout :value)})))


(defnc AutosizeInput
  [{:keys [value placeholder onBlur onFocus] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[[width] set-size!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        input (or _ref _input)
        dummy (hooks/use-ref nil)
        dummy-style (hooks/use-ref nil) 
        ;;
        [state set-state!] (hooks/use-state #{})
        ;;
        value (or value "")
        placeholder (or placeholder " ")
        [dw dh] (when @dummy
                  [(.-scrollWidth @dummy)
                   (.-scrollHeight @dummy)])]
    (letfn [(maybe-resize [target]
              (when target
                (let [[w h] [(.-scrollWidth target)
                             (.-scrollHeight target)]]
                  (when-not (< -5 (- width w) 5)
                    (set-size! [w h])))))]
      (hooks/use-effect
        [value]
        (maybe-resize @dummy))
      (hooks/use-effect
        :once
        (when @input
          (reset!
            dummy-style
            (merge
              (util/get-font-style @input)
              (util/css-attributes @input
                                   :padding :margin
                                   :padding-left :padding-top
                                   :padding-right :padding-bottom)))))
      (<>
        (d/input
          {:value (or value "")
           :placeholder placeholder
           :ref #(reset! (or _ref input) %)
           :onInput (fn [e]
                      (let [t (.-target e)
                            w (.-scrollWidth t)
                            h (.-scrollHeight t)]
                        (when (or
                                (not (< -5 (- w dw) 5))
                                (not (< -5 (- h dh) 5)))
                          (set-size! [w h]))))
           & (->
               props
               (dissoc :value :placeholder :ref :autoresize)
               (assoc-in [:style :width] width))})
        (d/div
          {:ref (fn [e] (reset! dummy e))
           :style (merge
                    @dummy-style
                    {:position "absolute"
                     :font-familly "inherit"
                     :top 0 :left 0
                     :visibility "hidden"
                     :height 0 :overflow "scroll"
                     :white-space "pre"})
           :autoComplete "off"
           :autoCorrect "off"
           :spellCheck "false"
           :autoCapitalize "false"
           & (dissoc props :value :autoresize :className)}
          (cond
            ((every-pred string? not-empty) value) value
            (number? value) value
            :else placeholder))))))

(defn ->number [x]
  (let [x' (if (number? x) x
               (if (string? x) (clojure.edn/read-string x)
                   (throw
                    (ex-info "Cannot cast to number"
                             {:value x}))))]
    (if (number? x')
      x'
      (throw
       (ex-info (str "Cannot cast " (pr-str x) " to number")
                {:value x})))))

(defnc NumberInput
  [{:keys [value on-blur onBlur onFocus on-focus placeholder] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[{:keys [width style]} update-state!] (hooks/use-state nil)
        on-blur (or onBlur on-blur identity)
        on-focus (or onFocus on-focus identity)
        [focused? set-focused!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        input (or _ref _input)
        dummy (hooks/use-ref nil)
        translate (use-translate)
        value (if (some? value)
                (if (not focused?)
                  (translate value)
                  (str value))
                "")
        placeholder (or placeholder " ")]
    (hooks/use-effect
     :always
     (when @input
       (let [style' (merge
                     (util/get-font-style @input)
                     (util/css-attributes  @input :padding :margin))]
         (when (not= style style')
           (update-state! assoc :style style')))))
    ; (hooks/use-layout-effect
    (hooks/use-effect
     :always
     (when @dummy
       (update-state! assoc :width (+ 8 (.-scrollWidth @dummy)))))
    (<>
     (d/input
      {:value (or value "")
       :placeholder placeholder
       :ref #(reset! (or _ref input) %)
       & (->
          props
          (dissoc :value :placeholder :ref)
          (assoc-in [:style :width] width)
          (assoc
               ;;
           :onBlur (fn [e]
                     (set-focused! false)
                     (on-blur e))
               ;;
           :onFocus (fn [e]
                      (set-focused! true)
                      (on-focus e))))})
     (d/div
      {:ref #(reset! dummy %)
       :style (merge
               style
               {:position "absolute"
                :top 0 :left 0
                :visibility "hidden"
                :height 0 :overflow "scroll"
                :white-space "pre"})
       & (dissoc props :value)}
      (cond
        ((every-pred string? not-empty) value) value
        (number? value) value
        :else placeholder)))))


(defnc TextAreaElement [{:keys [value
                                placeholder
                                onChange
                                onFocus
                                onBlur
                                className]
                         :or {onChange identity}
                         :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[[width height] set-size!] (hooks/use-state nil)
        [dummy-style set-dummy-style!] (hooks/use-state nil)
        ; [{:keys [height style]} update-state!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        input (or _ref _input)
        dummy (hooks/use-ref nil)
        ;;
        [focused? set-focused!] (hooks/use-state nil)
        ;;
        [_ pr _ pl]
        (hooks/use-memo
         [dummy-style]
         (when dummy-style
           (map
            #(js/parseInt (re-find #"\d+" %))
            [(:padding-top dummy-style)
             (:padding-right dummy-style)
             (:padding-bottom dummy-style)
             (:padding-left dummy-style)])))
        [dw dh] (when @dummy
                  [(.-scrollWidth @dummy)
                   (.-scrollHeight @dummy)])]
    (hooks/use-effect
     :always
     (when (and height @dummy)
       (let [dh (.-scrollHeight @dummy)
             dw (.-scrollWidth @dummy)]
         (when-not (< -5 (- height dh) 5)
           (set-size! [dw dh])))))
    (hooks/use-effect
      :once
      (when @input
        (let [[w h] [(.-scrollWidth @input)
                     (.-scrollHeight @input)]]
          (set-size! [w h]))
        (set-dummy-style!
          (merge
            (util/get-font-style @input)
            (util/css-attributes @input
                                 :padding :margin
                                 :padding-left :padding-top
                                 :padding-right :padding-bottom)))))
    (<>
     (d/textarea
      {:className className
       :onChange onChange
       :style {:width width :height height}
       :onInput (fn [e]
                  (let [t (.-target e)
                        w (.-scrollWidth t)
                        h (.-scrollHeight t)]
                    (when (or
                            (not (< -5 (- w dw) 5))
                            (not (< -5 (- h dh) 5)))
                      (set-size! [w h]))))
       :onBlur (fn [e]
                 (set-focused! false)
                 (when (fn? onBlur) (onBlur e)))
       :onFocus (fn [e]
                  (set-focused! true)
                  (when (fn? onFocus) (onFocus e)))
       :value (or value "")
       :ref #(reset! input %)
       & (->
          props
          (dissoc :style :value :className :ref :onChange)
          (update :style merge {:height (inc height)
                                :overflow "hidden"}))})
     (when focused?
       (d/pre
         {:ref #(reset! dummy %)
          :className className
          :style (merge
                   dummy-style
                   (cond-> {:position "fixed"
                            :top 0 :left 0
                            :visibility "hidden"
                            :white-space "pre-wrap"
                            :word-break "break-word"
                            :font-family "inherit"}
                     @input (assoc :width (- (.-scrollWidth @input) pl pr))))}
         (str (if (not-empty value) value  placeholder) \space))))))


(defnc SliderElement
  [{:keys [min max value disabled className
           onChange
           onFocus
           onBlur
           orient]
    :or {min 0
         max 1
         orient "horizontal"}}]
  (let [value (or value 0)]
    (d/input
     {:className className
      :orient orient
      :type "range"
      :value value
      :disabled disabled
      :min min
      :max max
      :onBlur onBlur
      :onFocus onFocus
      :onChange onChange})))



(defnc Mask [props]
  (let [props' (use-mask props)]
    (d/div
      {:class "eywa-mask-field"}
      (d/input {& props'}))))


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
