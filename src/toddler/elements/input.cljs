(ns toddler.elements.input
  (:require
    clojure.string
    clojure.edn
    [vura.core :as vura]
    ["react" :as react]
    [cljs.core.async :as async]
    [helix.core :refer [defnc factory $ <>]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [toddler.util :as util]
    [toddler.hooks :refer [use-translate]]
    [toddler.i18n.number :refer [number-formatter]]))


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

(defnc InputElement [{:keys [value placeholder class on-change]
                      :or {on-change identity}
                      :as props}]
  (let [[{:keys [width
                 style]} update-state!] (hooks/use-state nil)
        input (hooks/use-ref nil)
        dummy (hooks/use-ref nil)
        value (or value "")
        placeholder (or placeholder " ")
        class' (clojure.string/join
                 " "
                 (cond-> ["element"]
                   (seq? class) (concat class)
                   (string? class) (conj class)))] 
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
    (d/div
     {:class (clojure.string/join
               " " 
               (cond-> ["autoresize-input"]
                 (:disabled props) (conj "disabled")))}
     (d/input 
       {:value (or value "")
        :placeholder placeholder
        :autoComplete "off"
        :autoCorrect "off"
        :spellCheck "false"
        :autoCapitalize "false"
        :className class' 
        :ref #(reset! input %)
        :style {:width width}
        :on-change on-change
        & (dissoc props :value :class :placeholder :style :autoresize "autoresize")})
     (d/div
       {:ref #(reset! dummy %)
        :style (merge 
                 style 
                 {:position "absolute"
                  :top 0 :left 0
                  :visibility "hidden"
                  :height 0 :overflow "scroll"
                  :white-space "pre"})
        & (dissoc props :value :class :placeholder :style :autoresize "autoresize")}
       (cond
         ((every-pred string? not-empty) value) value
         (number? value) value
         :else placeholder)))))

(def input (factory InputElement))

(defnc AutosizeInput 
  [{:keys [value placeholder] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[{:keys [width
                 style]
          :or {width 30}} update-state!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        input (or _ref _input)
        dummy (hooks/use-ref nil)
        value (or value "")
        placeholder (or placeholder " ")] 
    (hooks/use-effect
      :always
      (when @input
        (let [style' (merge
                      (util/get-font-style @input)
                      (util/css-attributes  @input :padding :margin))]
          (when (not= style style')
            (update-state! assoc :style style'))))
      (when @dummy 
        (let [width' (+ 8 (.-scrollWidth @dummy))]
          (when-not (= width width')
            (update-state! assoc :width width')))))
    (<>
      (d/input 
        {:value (or value "")
         :placeholder placeholder
         :ref #(reset! (or _ref input) %)
         & (->
             props
             (dissoc :value :placeholder :ref :autoresize)
             (assoc-in [:style :width] width))})
      (d/div
        {:ref #(reset! dummy %)
         :style (merge 
                  style 
                  {:position "absolute"
                   :font-familly "inherit"
                   :top 0 :left 0
                   :visibility "hidden"
                   :height 0 :overflow "scroll"
                   :white-space "pre"})
         & (dissoc props :value :autoresize :className)}
        (cond
          ((every-pred string? not-empty) value) value
          (number? value) value
          :else placeholder)))))

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
                  (translate :tongue/format-number value)
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


(defnc NumberElement 
  [{:keys [disabled autoresize locale value on-blur on-focus] 
    :or {locale "en"
         on-blur identity
         on-focus identity}
    :as props}]
  (let [[focused? set-focused!] (hooks/use-state nil)
        formatter (hooks/use-memo
                    [locale]
                    (number-formatter locale))
        props' (if focused? 
                 props
                 (assoc props :value 
                        (if (some? value) 
                          (formatter value)
                          "")))
        props'' (cond-> 
                  (dissoc props' :formatter)
                  ;;
                  (not disabled) 
                  (assoc
                    :on-blur #(do
                                (set-focused! false)
                                (on-blur %))
                    :on-focus #(do
                                 (set-focused! true)
                                 (on-focus %))))] 
    (if autoresize
      ($ InputElement 
         {& (dissoc props'' :autoresize)})
      (d/input 
        {& (dissoc props'' :autoresize)}))))

(def number (factory NumberElement))

(defnc TextAreaElement [{:keys [value
                                placeholder
                                onChange
                                className]
                         :or {onChange identity}
                         :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[{:keys [height
                 style]} update-state!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        input (or _ref _input)
        dummy (hooks/use-ref nil)
        ;;
        [_ pr _ pl] 
        (hooks/use-memo
          [style]
          (when style
            (map 
              #(js/parseInt (re-find #"\d+" %))
              [(:padding-top style)
               (:padding-right style)
               (:padding-bottom style)
               (:padding-left style)])))]
    (hooks/use-effect
      :always
      (when @dummy
        (let [scroll-height (vura/round-number (.-scrollHeight @dummy) 1 :up)]
          (when-not (= height scroll-height) 
            (update-state! 
              assoc 
              :height scroll-height
              :style (merge 
                       (util/get-font-style @input)
                       (util/css-attributes @input 
                                            :padding :margin 
                                            :padding-left :padding-top 
                                            :padding-right :padding-bottom)))))))
    (<>
      (d/textarea
        {:className className
         :onChange onChange 
         :value (or value "")
         :style style
         :ref #(reset! input %)
         & (->
             props
             (dissoc :style :value :className :ref :onChange)
             (update :style merge {:height (inc height)
                                   :overflow "hidden"}))})
      (d/pre
        {:ref #(reset! dummy %)
         :className className
         :style (merge 
                  style
                  (cond-> {:position "fixed"
                           :top 0 :left 0
                           :visibility "hidden"
                           :white-space "pre-wrap"
                           :word-break "break-word"
                           :font-family "inherit"}
                    @input (assoc :width (- (.-scrollWidth @input) pl pr))))}
        (str (if (not-empty value) value  placeholder) \space)))))

;(defnc TextAreaElement [{:keys [value
;                                placeholder
;                                onChange
;                                className]
;                         :or {onChange identity}
;                         :as props}]
;  (let [[{:keys [height
;                 style]} update-state!] (hooks/use-state nil)
;        value' (hooks/use-ref value)
;        [cached update!] (use-idle 
;                           value 
;                           (fn [v]
;                             (let [v (if (or (= v :NULL) (empty? v)) nil v)]
;                               (when-not (= @value' v) 
;                                 (println "RECEIVED VALUE: " v)
;                                 (onChange v))))
;                           1000)
;        input (hooks/use-ref nil)
;        dummy (hooks/use-ref nil)
;        ;;
;        [_ pr _ pl] 
;        (hooks/use-memo
;          [style]
;          (when style
;            (map 
;              #(js/parseInt (re-find #"\d+" %))
;              [(:padding-top style)
;               (:padding-right style)
;               (:padding-bottom style)
;               (:padding-left style)])))]
;    (hooks/use-effect
;      :always
;      (when @dummy
;        (let [scroll-height (vura/round-number (.-scrollHeight @dummy) 1 :up)]
;          (when-not (= height scroll-height) 
;            (update-state! 
;              assoc 
;              :height scroll-height
;              :style (merge 
;                       (util/get-font-style @input)
;                       (util/css-attributes @input 
;                                            :padding :margin 
;                                            :padding-left :padding-top 
;                                            :padding-right :padding-bottom)))))))
;    (hooks/use-effect
;      [value]
;      (reset! value' value)
;      (when (not= value cached) 
;        (println "TEXTAREA DIFF: " [value cached])
;        (update! value)))
;    (<>
;      (d/textarea
;        {:className className
;         :onChange (fn [e] 
;                     (println "TEXT CHANGE: " (.. e -target -value))
;                     (update! (.. e -target -value)))
;         :value (or cached "")
;         :ref #(reset! input %)
;         & (->
;             props
;             (dissoc :style :value :className :ref :onChange)
;             (update :style merge {:height (inc height)
;                                   :overflow "hidden"}))})
;      (d/pre
;        {:ref #(reset! dummy %)
;         :className className
;         :style (merge 
;                  style
;                  (cond-> {:position "fixed"
;                           :top 0 :left 0
;                           :visibility "hidden"
;                           :white-space "pre-wrap"
;                           :word-break "break-word"
;                           :font-family "inherit"}
;                    @input (assoc :width (- (.-scrollWidth @input) pl pr))))}
;        (or (not-empty cached) placeholder)))))


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

; (def slider-element (om/factory SliderElement))
