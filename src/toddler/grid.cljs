(ns toddler.grid
  (:require
   [vura.core :refer [round-number]]
   [toddler.layout :refer [*container-dimensions*]]
   [helix.core :refer [defnc defhook $ provider memo]]
   [helix.hooks :as hooks]
   [helix.children :as c]
   [helix.dom :as d]))

(comment
  (def breakpoints
    {:lg 1200
     :md 996
     :sm 768
     :xs 480
     :xxs 0})

  (def width 300))

(defn get-breakpoint-from-width
  [breakpoints width]
  (key (last (take-while #(< (val %) width) (sort-by val breakpoints)))))

(defnc GridItem
  [{:keys [x y width height className]
    [dx dy] :margin
    :or {dx 10 dy 10}
    :as props}]
  (let [{:keys [x y width height]
         :as container-dimensions}
        (hooks/use-memo
          [x y width height]
          (zipmap
           [:x :y :width :height]
           [(+ x dx) (+ y dy) (- width (* 2 dx)) (- height (* 2 dy))]))]
    (provider
     {:context *container-dimensions*
      :value container-dimensions}
     (d/div
      {:className className
       :style {:position "absolute"
               :transform (str "translate(" x "px," y "px)")
               :width width
               :height height
               :transition "transform .2s ease-in"}}
      (c/children props)))))

(defhook use-grid-data
  [{:keys [width breakpoints row-height columns layouts]}]
  (let [sorted-breakpoints (hooks/use-memo
                             [breakpoints]
                             (map key (sort-by val breakpoints)))
        ;;
        [breakpoint column-width]
        (hooks/use-memo
          [width]
          (let [b (last
                   (take-while
                    #(< (get breakpoints %) width)
                    sorted-breakpoints))
                column-count (get columns b)]
            [b (round-number (/ width column-count) 1 :down)]))
        ;;
        layouts (hooks/use-ref layouts)
        ;;
        [breakpoint layout]
        (hooks/use-memo
          [breakpoint]
          (when-some [[breakpoint layout]
                      (or
                       [breakpoint (get @layouts breakpoint)]
                       (some
                        (fn [breakpoint]
                          (when-some [layout (get @layouts breakpoint)]
                            [breakpoint layout]))
                        (reverse
                         (take
                          (inc (.indexOf sorted-breakpoints breakpoint))
                          sorted-breakpoints))))]
            [breakpoint
             (reduce
              (fn [r {:keys [i x y w h min-w max-w min-h max-h]}]
                (assoc r i
                       (cond->
                        {:x (* x column-width)
                         :y (* y row-height)
                         :width (* w column-width)
                         :height (* h row-height)}
                         min-h (assoc :minHeight min-h)
                         max-h (assoc :maxHeight max-h)
                         min-w (assoc :minWidth min-w)
                         max-w (assoc :maxWidth max-w))))
              nil
              layout)]))
        ;;
        height (hooks/use-memo
                 [layout]
                 (apply max
                        (map
                         (fn [{:keys [y height]}]
                           (+ y height))
                         (vals layout))))]
    {:width width
     :height height
     :layout layout}))

(letfn [(same? [a b]
          (let [ks [:width :breakpoints :row-height :margin :padding :columns :layouts]
                before (select-keys a ks)
                after (select-keys b ks)
                result (= before after)]
            result))]
  (defnc GridLayout
    {:wrap [(memo same?)]}
    [{:keys [width breakpoints
             row-height margin padding
             columns layouts className]
      :or {breakpoints {:lg 1200
                        :md 996
                        :sm 768
                        :xs 480
                        :xxs 0}
           columns {:lg 12
                    :md 10
                    :sm 6
                    :xs 4
                    :xxs 2}
           margin [10 10]
           padding margin
           width 1200
           row-height 30}
      :as props}]
    (let [{:keys [layout height]} (use-grid-data
                                   {:width width
                                    :margin margin
                                    :columns columns
                                    :breakpoints breakpoints
                                    :padding padding
                                    :layouts layouts
                                    :row-height row-height})]
      (when layout
        (d/div
         {:className (str "toddler-grid")
          :style
          {:width width
           :height height
           :position "relative"}}
         (map
          (fn [component]
            (let [k (.-key component)]
              ($ GridItem
                 {:key k :margin margin & (get layout k)}
                 component)))
          (c/children props)))))))
