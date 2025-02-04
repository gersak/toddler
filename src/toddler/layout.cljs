(ns toddler.layout
  (:require
   [helix.core
    :refer [defnc create-context
            defhook provider
            fnc $ memo]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [vura.core :as vura]
    ; [toddler.ui :refer [forward-ref]]
   [toddler.app :as app]
   [toddler.core
    :refer [use-dimensions]]))

(defhook ^:no-doc use-layout
  ([] (hooks/use-context app/layout))
  ([k] (get (hooks/use-context app/layout) k)))

(defhook use-m-columns
  "Hook will return data distributed to columns-count number
  of vectors, distributed by pattern that goes sequentialy through
  original data sequence and appending to column = mod(idx, coulumn-count)"
  [data columns-count]
  (hooks/use-memo
    [data columns-count]
    (loop [[c & r :as data] (seq data)
           idx 0
           result (vec (take columns-count (repeat [])))]
      (if (empty? data)
        result
        (recur r (mod (inc idx) columns-count) (update result idx conj c))))))

(defhook use-columns-frame
  "Hook will compute how many columns are visible at the moment by
  comparing column-width, container-width, maximum number of columns and
  padding between columns.
  
  Returns input props with additional props :column-count and :estimated-width"
  ([{:keys [column-width container-width max-columns padding-x]
     :or {column-width 400
          max-columns 4
          padding-x 32}
     :as props}]
   (let [estimated-size (vura/round-number container-width (+ column-width padding-x) :floor)
         column-count (hooks/use-memo
                        [column-width container-width]
                        (min (quot estimated-size column-width)
                             max-columns))]
     (assoc props
       :estimated-width estimated-size
       :column-count column-count))))

(def ^:dynamic ^js *container* (create-context nil))
(def ^:dynamic ^js *container-dimensions* (create-context nil))

(defhook use-container
  "Hook will return `*container` context value"
  []
  (hooks/use-context *container*))

(defhook use-container-dimensions "Hook will return `*container-dimensions*` value"
  []
  (hooks/use-context *container-dimensions*))

(letfn [(same? [a b]
          (let [ks [:style :className]
                before (select-keys a ks)
                after (select-keys b ks)
                result (= before after)]
            result))]
  (defnc Container
    "Function that will render div and track its dimensions.
    Try to use fixed size container, with known both width and
    height."
    [props]
    ;; TODO - maybe remove this
    ; {:wrap [(memo same?)]}
    (let [[container dimensions] (use-dimensions)]
      (d/div
       {:ref #(reset! container %)
        & props}
       (provider
        {:context *container-dimensions*
         :value dimensions}
        (provider
         {:context *container*
          :value container}
         (c/children props)))))))

(defn wrap-container
  "Function will wrap container around component. Use
  style or class to control container dimensions. Container
  will track its size and provide that data through `*container-dimensions*`
  context"
  ([component]
   (fnc Container [props]
     ($ Container ($ component {& props}))))
  ([component cprops]
   (fnc [props]
     ($ Container {& cprops} ($ component {& props})))))

(defn get-breakpoint-from-width
  [breakpoints width]
  (key (last (take-while #(< (val %) width) (sort-by val breakpoints)))))

(defnc GridItem
  "Component that will absolutely position grid element to
  x, y props with width and height. GridItem children will
  be rendered inside absolutely positioned and fixed size
  element."
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
  ""
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
            [b (vura/round-number (/ width column-count) 1 :down)]))
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
    "Component that will combine `use-grid-data` hook to
    compute layout and height of of grid definition and
    will put every child in GridItem component with
    props that define that grid position."
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
