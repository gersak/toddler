(ns toddler.elements.mask
  (:require 
    clojure.string
    [helix.dom :as d]
    [helix.core
     :refer [defnc defhook defhook]]
    [helix.hooks :as hooks]))

(defn replace-char [value position new-char]
  (let [[left right] (split-at position value)]
    (if (seq right)
      (apply str
             (apply str left)
             new-char
             (apply str (rest right)))
      (apply str
             (apply str (butlast left))
             new-char))))


(defhook use-mask
  [{:keys [on-change onChange on-key-down onKeyDown] 
    ovalue :value
    :as props}]
  (let [on-change (or onChange on-change identity)
        on-key-down (or onKeyDown on-key-down identity)
        [{:keys [value mask
                 delimiters 
                 constraints
                 caret-position]} update-state!] (hooks/use-state props)
        input (hooks/use-ref nil)]
    (hooks/use-effect
      [value caret-position]
      (when @input 
        (.setSelectionRange @input caret-position caret-position)))
    (hooks/use-effect
      [ovalue]
      (when-not (= value ovalue)
        (update-state! assoc :value ovalue)))
    (merge 
      props
      {:value value 
       :ref #(reset! input %)
       :on-key-down (fn [e]
                      (let [key-code (.-keyCode e)
                            char-code (.-key e)
                            caret-position (if @input 
                                             (.-selectionStart @input)
                                             0)]
                        (letfn [(move-forward [] 
                                  (or 
                                    (first
                                      (keep
                                        #(when-not (delimiters (get mask %)) %)
                                        (range (inc caret-position) (inc (count mask)))))
                                    caret-position))
                                (move-backward [] 
                                  (or
                                    (first
                                      (keep
                                        #(when-not (delimiters (get mask (dec %))) %)
                                        (range (dec caret-position) -1 -1)))
                                    caret-position))] 
                          (update-state!
                            merge
                            (case key-code
                              ;; Backspace
                              8 (when (pos? caret-position)
                                  (.preventDefault e)
                                  (let [new-val (get mask (dec caret-position))
                                        value' (replace-char value (dec caret-position) new-val)]
                                    (when (fn? on-change) (on-change value'))
                                    {:value value'
                                     :caret-position (move-backward)}))
                              ;; DELETE
                              46 (when (< (count value) caret-position)
                                   (.preventDefault e)
                                   (let [new-val (get mask (inc caret-position))
                                         value' (replace-char value caret-position new-val)]
                                     (when (fn? on-change) (on-change value'))
                                     {:value value'
                                      :caret-position (move-forward)}))
                              ;; Arrows
                              37 {:caret-position (dec caret-position)}
                              38 {:caret-position (count mask)}
                              39 {:caret-position (inc caret-position)}
                              40 {:caret-position 0}
                              ;; TAB + special keys
                              (9 13 17 18 19 20) nil
                              (do
                                (.preventDefault e)
                                (when-not (delimiters (get value caret-position)) 
                                  (let [value' (replace-char value caret-position char-code)
                                        p (re-pattern 
                                            (str \[ (apply str delimiters) \] \+))
                                        mask-parts (clojure.string/split mask p)
                                        value-parts (clojure.string/split value' p)]
                                    (when (every? identity (map 
                                                             ;; Either constraint matches value part or
                                                             ;; value part is untuched (= mask)
                                                             (fn [c v m] (or (re-find c v) (= v m))) 
                                                             constraints value-parts mask-parts))
                                      (when (and 
                                              (fn? on-change)
                                              (not= value' value)) 
                                        (on-change value'))
                                      {:value value'
                                       :caret-position (move-forward)}))))))))
                      (on-key-down e))})))

(defnc Mask [props]
  (let [props' (use-mask props)]
    (d/div
      {:class "eywa-mask-field"}
      (d/input {& props'}))))


(.log js/console "Loaded toddler.elements.mask")
