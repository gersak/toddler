(ns toddler.input
  (:require
   goog.object
   clojure.string
   clojure.edn
   ["react" :as react]
   [cljs.core.async :as async]
   [helix.core :refer [defnc <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.core :as toddler]
   [toddler.util :as util]))

(defnc IdleInput
  {:wrap [(react/forwardRef)]}
  [{:keys [value
           timeout
           onChange]
    :or {timeout 500}
    :as props}
   _ref]
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
  {:wrap [(react/forwardRef)]}
  [{:keys [value placeholder] :as props} _ref]
  (let [[[width] set-size!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        input (or _ref _input)
        dummy (hooks/use-ref nil)
        dummy-style (hooks/use-ref nil)
        ;;
        value (or value "")
        placeholder (or placeholder " ")
        [dw dh] (when @dummy
                  [(.-scrollWidth @dummy)
                   (.-scrollHeight @dummy)])
        [initialized? set-initialized!] (hooks/use-state false)
        focused? (and @input (= @input (.-activeElement js/document)))]
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
       (when (or
              (not initialized?)
              focused?)
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
            :else placeholder)))))))

(defnc TextAreaElement
  {:wrap [(react/forwardRef)]}
  [{:keys [placeholder
           className
           onChange
           on-change]
    upstream-value :value
    :or {onChange identity}
    :as props} _ref]
  (let [on-change (or on-change onChange)
        [dummy-style set-dummy-style!] (hooks/use-state nil)
        _input (hooks/use-ref nil)
        value (hooks/use-ref (or upstream-value ""))
        input (or _ref _input)
        dummy (hooks/use-ref nil)
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
        [initialized? set-initialized!] (hooks/use-state false)
        [focused? set-focused!] (hooks/use-state false)
        delayed-focused? (toddler/use-delayed focused?)
        delayed-initialized? (toddler/use-delayed initialized?)
        show-dummy? (or
                     (not initialized?)
                     (not delayed-initialized?)
                     focused?
                     delayed-focused?)]
    (letfn [(refresh [dummy target]
              (when (and dummy target)
                (let [th (.-clientHeight target)
                      dh (.-scrollHeight dummy)
                      dw (.-scrollWidth dummy)]
                  (when-not (< -5 (- th dh) 5)
                    (set! (.. target -style -height) (str dh "px"))))))]
      (hooks/use-effect
        :once
        (when @input
          (set-dummy-style!
           (merge
            (util/get-font-style @input)
            (util/css-attributes @input
                                 :padding :margin
                                 :padding-left :padding-top :line-height
                                 :padding-right :padding-bottom)))))
      (hooks/use-effect
        [upstream-value]
        (when (not= upstream-value @value)
          (set-initialized! false)
          (reset! value upstream-value)))
      (<>
       (d/textarea
        {:className className
         :on-focus (fn [_] (set-focused! true))
         :on-blur (fn [_] (set-focused! false))
         :on-change (fn [e]
                      (let [_value (when e (not-empty (.. e -target -value)))]
                        (reset! value _value)
                        (on-change _value)))
         :value (or upstream-value "")
         :ref #(reset! input %)
         & (->
            props
            (dissoc :style :value :className :ref :onChange :on-change)
            (update :style merge {:overflow "hidden"}))})
       (when show-dummy?
         (d/pre
          {:ref (fn [el]
                  (do
                    (reset! dummy el)
                    (when (and
                           (not initialized?)
                           (not focused?))
                      (set-initialized! true))
                    (refresh el @input)))
           :key :dummy
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
          (str (if (not-empty upstream-value) upstream-value  placeholder) \space)))))))
