(ns toddler.layout
  (:require
   [clojure.core.async :as async]
   [helix.core
    :refer [defnc create-context
            defhook provider
            fnc $ memo <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [vura.core :as vura]
   [toddler.core :as toddler
    :refer [use-dimensions]]
   [toddler.util :as util]
   [toddler.ui :as ui]
   [toddler.app :as app]))

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

(defonce ^:no-doc ^:dynamic *tabs* (create-context))

#_(defnc use-tabs
    {:wrap [(ui/forward-ref)]}
    [{:keys [class className]
      :as props} _ref]
    (let [tabs-target (hooks/use-ref nil)
          _tabs (hooks/use-ref nil)
          _tabs (or _ref _tabs)
          [selected on-select!] (hooks/use-state nil)
          [available set-available!] (toddler/use-idle
                                      nil (fn [tabs]
                                            (on-select!
                                             (fn [id]
                                               (when-not (= tabs :NULL)
                                                 (if-not (nil? id) id
                                                         (ffirst tabs))))))
                                      {:initialized? true})
          register (hooks/use-callback
                     [selected]
                     (fn register
                       ([tab] (register tab tab nil))
                       ([tab order] (register tab tab order))
                       ([id tab order]
                        (set-available!
                         (fn [tabs]
                           (vec (sort-by #(nth % 2) (conj tabs [id tab order]))))))))
          unregister (hooks/use-callback
                       [selected]
                       (fn [tab]
                         (set-available!
                          (fn [tabs]
                            (vec
                             (sort-by
                              #(nth % 2)
                              (remove
                               (fn [[_ _tab _]]
                                 (= tab _tab))
                               tabs)))))))
          update-tab (hooks/use-callback
                       [selected]
                       (fn [key tab order]
                         (set-available!
                          (fn [tabs]
                            (let [next (mapv
                                        (fn [[k t o]]
                                          (if (= key k) [k tab order]
                                              [k t o]))
                                        tabs)]
                              next)))))
          tabs (map #(take 2 %) available)
          container-dimensions (use-container-dimensions)
          [_ {tabs-height :height}] (toddler/use-dimensions _tabs)
          tab-content-dimensions  (hooks/use-memo
                                    [(:height container-dimensions) tabs-height]
                                    (assoc container-dimensions :height
                                           (- (:height container-dimensions)
                                              tabs-height)))
          translate (toddler/use-translate)
          tab-elements (hooks/use-ref nil)
        ;;
          {marker-top :top
           marker-left :left
           marker-height :height
           marker-width :width}
          (hooks/use-memo
            [selected]
            (if-not selected
              {:top 0 :left 0}
              (if-some [selected-el (get @tab-elements selected)]
                (let [[left top width height] (util/dom-dimensions selected-el)
                      [tabs-left tabs-top] (util/dom-dimensions @_tabs)
                      top (- top tabs-top)
                      left (- left tabs-left)]
                  {:top top :left left
                   :width width :height height})
                {:top 0 :left 0})))]
      (<>
       (d/div
        {:ref _tabs
         :class (cond->
                 (list "toddler-tabs" $tabs)
                  className (conj className)
                  (string? class) (conj class)
                  (sequential? class) (into class))}
        (d/div
         {:ref #(swap! tab-elements assoc ::marker %)
          :style {:top marker-top :left marker-left
                  :width marker-width :height marker-height}
          :className (css
                      :z-0
                      :absolute
                      :rounded-md
                      {:transition "width .2s ease-in-out, height .2s ease-in-out, left .2s ease-in-out"
                       :background-color "var(--tab-selected-bg)"})})
        (d/div
         {:ref #(reset! tabs-target %)
          :class ["tabs"]}
         (map
          (fn [[id tab]]
            (d/div
             {:key tab
              :ref #(swap! tab-elements assoc id %)
              :class (cond->
                      (list "toddler-tab" $tab)
                       (= id selected) (conj "selected")
                       className (conj className)
                       (string? class) (conj class)
                       (sequential? class) (into class))
              :on-click (fn [] (on-select! id))}
             (if (string? tab) tab
                 (translate tab))))
          tabs)))
       (provider
        {:context tabs-context
         :value {:register register
                 :unregister unregister
                 :update update-tab
                 :select! on-select!
                 :selected selected}}
        (provider
         {:context layout/*container-dimensions*
          :value tab-content-dimensions}
         (d/div
          {:className "tab-content"}
          (c/children props)))))))

(defhook use-tabs
  "Hook that will create logic for tab registration. Its only valid argument is
  ref that will be attached to tabs component and it is optioinal.
  
  Hook will return map with following keys:
  
   * :tab/refs       - mapping of tab :id to tab dom element
   * :tab/marker     - dimensions of currently selected tab `[:top :left :width :height]`
   * :tab/dimensions - dimensions for tab content. It is value of:
                       `*container-dimensions* - tabs element height = content-height`
   * :tabs/ref        - ref that should be attached to tabs element, so that this hook
                       can track tabs dimensions
   * :tabs           - sequence of tabs that are registered in form of `[id name]`
   * :tabs/context   - context important for [[tab]] component to function well. It holds
                       functions that will :register, :unregister, :update, :select! tab
                       as well as value of :selected that holds :id of selected tab
                      
  **IMPORTANT** - use helix.core/provider to provide :tabs/context received from
  this hook to children through `*tabs*` context. Just pass it as is..."
  ([] (use-tabs nil))
  ([_ref]
   (let [_tabs (hooks/use-ref nil)
         _tabs (or _ref _tabs)
         [selected on-select!] (hooks/use-state nil)
         [available set-available!] (toddler/use-idle
                                     nil (fn [tabs]
                                           (on-select!
                                            (fn [id]
                                              (when-not (= tabs :NULL)
                                                (if-not (nil? id) id
                                                        (ffirst tabs))))))
                                     {:initialized? true})
         register (hooks/use-callback
                    [selected]
                    (fn register
                      ([tab] (register tab tab nil))
                      ([tab order] (register tab tab order))
                      ([id tab order]
                       (set-available!
                        (fn [tabs]
                          (vec (sort-by #(nth % 2) (conj tabs [id tab order]))))))))
         unregister (hooks/use-callback
                      [selected]
                      (fn [tab]
                        (set-available!
                         (fn [tabs]
                           (vec
                            (sort-by
                             #(nth % 2)
                             (remove
                              (fn [[_ _tab _]]
                                (= tab _tab))
                              tabs)))))))
         update-tab (hooks/use-callback
                      [selected]
                      (fn [key tab order]
                        (set-available!
                         (fn [tabs]
                           (let [next (mapv
                                       (fn [[k t o]]
                                         (if (= key k) [k tab order]
                                             [k t o]))
                                       tabs)]
                             next)))))
         tabs (map #(take 2 %) available)
         container-dimensions (use-container-dimensions)
         [_ {tabs-height :height}] (toddler/use-dimensions _tabs)
         tab-content-dimensions  (hooks/use-memo
                                   [(:height container-dimensions) tabs-height]
                                   (assoc container-dimensions :height
                                          (- (:height container-dimensions)
                                             tabs-height)))
         tab-elements (hooks/use-ref nil)
         ;;
         marker
         (hooks/use-memo
           [selected]
           (if-not selected
             {:top 0 :left 0}
             (if-some [selected-el (get @tab-elements selected)]
               (let [[left top width height] (util/dom-dimensions selected-el)
                     [tabs-left tabs-top] (util/dom-dimensions @_tabs)
                     top (- top tabs-top)
                     left (- left tabs-left)]
                 {:top top :left left
                  :width width :height height})
               {:top 0 :left 0})))]
     {:tab/refs tab-elements
      :tab/marker marker
      :tab/dimensions tab-content-dimensions
      :tabs/ref _tabs
      :tabs tabs
      :tabs/context {:register register
                     :unregister unregister
                     :update update-tab
                     :select! on-select!
                     :selected selected}})))

(defnc tab
  "Reusable component that will look for `*tabs*` context
  and render children if :id received in props matches
  :selected value in `*tabs*` context"
  [{:keys [name tab id focus? position] :as props
    :or {id tab}}]
  (let [tab (or name tab)
        {:keys [select!
                selected
                register
                unregister
                update]} (hooks/use-context *tabs*)]
    (hooks/use-effect
      :once
      (register id tab position)
      (when focus?
        (async/go
          (async/<! (async/timeout 1000))
          (select! id)))
      (fn []
        (unregister id)))
    (hooks/use-effect
      [tab]
      (update id tab position))
    (when (= id selected)
      (c/children props))))

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
