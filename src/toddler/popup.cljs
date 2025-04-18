(ns toddler.popup
  "Popup namespace that offers components and functions that
  focus on popup placement.
  
  Use Container component from this
  namespace as close to mounted react element as possible.
  
  All popup elements will look for Container component
  and render at that DOM element.
  
  There are few preference position sequences available
  
  ```clojure
  (def default-preference
    [#{:bottom :left}
     #{:bottom :right}
     #{:top :left}
     #{:top :right}
     #{:bottom :center}
     #{:top :center}
     #{:left :center}
     #{:right :center}])


  ```
  In addition to default-preferences other available preferences are:
  
   * central-preference
   * left-preference
   * right-preference
   * cross-preference
  
  Following components and hooks should be used in most cases:
   
   * [[Container]]
   * [[Area]]
   * [[Element]]
   * [[use-outside-action]]"
  (:require
   clojure.string
   [clojure.core.async :as async]
   ; [taoensso.telemere :as t]
   ["react" :as react]
   ["react-dom" :as rdom]
   [helix.core
    :refer [defnc fnc provider <>
            defhook create-context $]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [toddler.ui :refer [forward-ref]]
   [toddler.layout :refer [*container-dimensions*]]
   [toddler.util :as util]))

(def ^{:dynamic true
       :doc "Area element context. Area element is DOM element that
            will cause popup to appear on user event"}
  ^js *area-element*
  (create-context))

(def ^{:dynamic true
       :doc "Position of popup. Value should be set of max two keywords
            
             * :top
             * :bottom
             * :left
             * :right
             * :center"}
  ^js *position*
  (create-context))

(def ^{:dynamic true
       :doc "Is container context. Container is component where popups will
            be mounted using reacts **createPortal** function."}
  ^js *container* (create-context))
(def ^{:dynamic true
       :doc "Context for outside action channel. This channel will receive
            events when scroll or click outside happened"}
  ^js *outside-action-channel* (create-context))

(def ^{:doc "Sequence of prefered positions. First :bottom
           positions are tested, than :top positions and
           finally central"}
  default-preference
  [#{:bottom :left}
   #{:bottom :right}
   #{:top :left}
   #{:top :right}
   #{:bottom :center}
   #{:top :center}
   #{:left :center}
   #{:right :center}])

(def ^:no-doc central-preference
  [#{:bottom :center}
   #{:top :center}
   #{:bottom :left}
   #{:bottom :right}
   #{:top :left}
   #{:top :right}
   #{:left :center}
   #{:right :center}])

(def ^:no-doc left-preference
  [#{:bottom :left}
   #{:top :left}
   #{:bottom :center}
   #{:top :center}
   #{:bottom :right}
   #{:top :right}
   #{:left :center}
   #{:right :center}])

(def ^:no-doc right-preference
  [#{:bottom :right}
   #{:top :right}
   #{:bottom :center}
   #{:top :center}
   #{:top :left}
   #{:bottom :left}
   #{:left :center}
   #{:right :center}])

(def ^:no-doc cross-preference
  [#{:bottom :center}
   #{:right :center}
   #{:left :center}
   #{:top :center}])

(def ^{:doc "Offset context. How much in px is offset
            for popup from PopupArea element"
       :dynamic true}
  ^js *offset* 6)

(defmulti compute-candidate
  "Function will compute candidate for popup
  element bounding rect by specifying prefered
  position. I.E. for #{:bottom :left} position
  function will enrich input data with keys
  
   * :position/left
   * :position/right
   * :position/top
   * :position/bottom
   * :overflow/top
   * :overflow/bottom
   * :overflow/left
   * :overflow/right"
  (fn [{:keys [position]}] position))

(defmethod compute-candidate #{:bottom :left}
  [{:keys [popup-height
           popup-width
           window-width
           window-height
           left
           bottom]
    :as data}]
  (let [rp (+ left popup-width)
        dr (- window-width rp)
        bp (+ popup-height bottom *offset*)
        db (- window-height bp)]
    (assoc data
      :position/left left
      :position/top (+ bottom *offset*)
      :position/bottom bp
      :position/right rp
      :overflow/top 0
      :overflow/left (min 0 dr)
      :overflow/right 0
      :overflow/bottom (min 0 db))))

(defmethod compute-candidate #{:bottom :right}
  [{:keys [popup-height
           popup-width
           window-height
           right
           bottom]
    :as data}]
  (let [dl (- right popup-width)
        bp (+ popup-height bottom *offset*)
        db (- window-height bp)]
    (assoc data
      :position/left dl
      :position/top (+ bottom *offset*)
      :position/bottom bp
      :position/right right
      :overflow/top 0
      :overflow/left (min 0 dl)
      :overflow/right 0
      :overflow/bottom (min 0 db))))

(defmethod compute-candidate #{:top :left}
  [{:keys [popup-height
           popup-width
           window-width
           top
           left]
    :as data}]
  (let [dt (- top popup-height *offset*)
        rp (+ left popup-width)
        dr (- window-width rp)]
    (assoc data
      :position/left left
      :position/top dt
      :position/bottom (- top *offset*)
      :position/right rp
      :overflow/top (min 0 dt)
      :overflow/left 0
      :overflow/right (min 0 dr)
      :overflow/bottom 0)))

(defmethod compute-candidate #{:top :right}
  [{:keys [popup-height
           popup-width
           top
           right]
    :as data}]
  (let [dt (- top popup-height *offset*)
        dl (- right popup-width)]
    (assoc data
      :position/left dl
      :position/top dt
      :position/bottom (- top *offset*)
      :position/right right
      :overflow/top (min 0 dt)
      :overflow/left (min 0 dl)
      :overflow/right 0
      :overflow/bottom 0)))

(defmethod compute-candidate #{:top :center}
  [{:keys [half-popup-width
           popup-height
           horizontal-center
           window-width
           top]
    :as data}]
  (let [dt (- top popup-height *offset*)
        dl (- horizontal-center half-popup-width)
        rp (+ horizontal-center half-popup-width)
        dr (- window-width rp)]
    (assoc data
      :position/left dl
      :position/top dt
      :position/bottom (- top *offset*)
      :position/right rp
      :overflow/top (min 0 dt)
      :overflow/left (min 0 dl)
      :overflow/right (min 0 dr)
      :overflow/bottom 0)))

(defmethod compute-candidate #{:bottom :center}
  [{:keys [half-popup-width
           popup-height
           horizontal-center
           window-height
           window-width
           bottom]
    :as data}]
  (let [bp (+ popup-height bottom *offset*)
        db (- window-height bp)
        dl (- horizontal-center half-popup-width)
        rp (+ horizontal-center half-popup-width)
        dr (- window-width rp)]
    (assoc data
      :position/left dl
      :position/top (+ bottom *offset*)
      :position/bottom bp
      :position/right rp
      :overflow/top 0
      :overflow/left (min 0 dl)
      :overflow/right (min 0 dr)
      :overflow/bottom (min 0 db))))

(defmethod compute-candidate #{:left :center}
  [{:keys [half-popup-height
           popup-width
           vertical-center
           window-height
           window-width
           left]
    :as data}]
  (let [bp (+ vertical-center half-popup-height)
        tp (- vertical-center half-popup-height)
        rp (- left *offset*)
        lp (- left popup-width *offset*)
        db (- window-height bp)
        dr (- window-width rp)]
    (assoc data
      :position/left lp
      :position/top tp
      :position/bottom bp
      :position/right rp
      :overflow/top (min 0 tp)
      :overflow/left (min 0 lp)
      :overflow/right (min 0 dr)
      :overflow/bottom (min 0 db))))

(defmethod compute-candidate #{:right :center}
  [{:keys [half-popup-height
           popup-width
           vertical-center
           window-height
           window-width
           right]
    :as data}]
  (let [bp (+ vertical-center half-popup-height)
        tp (- vertical-center half-popup-height)
        rp (+ right popup-width *offset*)
        lp (+ right *offset*)
        db (- window-height bp)
        dr (- window-width rp)]
    (assoc data
      :position/left lp
      :position/top tp
      :position/bottom bp
      :position/right rp
      :overflow/top (min 0 tp)
      :overflow/left (min 0 lp)
      :overflow/right (min 0 dr)
      :overflow/bottom (min 0 db))))

(defn computation-props
  "For given target (popup area) element and popup element function
  will return map with keys that are important for popup position
  computation:
  
   * :top, :left, :right :bottom keys
   * :popup-width, :popup-height
   * :half-popup-width, :half-popup-height
   * :vertical-center, horizontal-center
   * :window-width :window-height"
  [target el]
  (when-let [[top left right bottom] (util/window-element-position target)]
    (when-let [[_ _ _ _ popup-width popup-height] (util/get-element-rect el)]
      {:top top :left left :right right :bottom bottom
       :popup-width popup-width
       :popup-height popup-height
       :half-popup-height (/ popup-height 2)
       :half-popup-width (/ popup-width 2)
       :vertical-center (+ top (/ (- bottom top) 2))
       :horizontal-center (+ left (/ (- right left) 2))
       :window-width (.-innerWidth js/window)
       :window-height (.-innerHeight js/window)})))

(defn ok-candidate?
  "Returns true if candidate didn't overflow. Than this
  position is OK"
  [{:keys [overflow/top
           overflow/bottom
           overflow/left
           overflow/right]
    :as candidate}]
  (when (not-any? neg? [top bottom left right])
    candidate))

(defn adjust-scroll-width
  "Keep in mind that scroll can appear. This function will
  move computed candidate for scroll-width"
  ([data] (adjust-scroll-width data 15))
  ([{:keys [position] :as data} scroll-width]
   (letfn [(move-right [d] (+ d scroll-width))
           (move-left [d] (- d scroll-width))
           (center-left [d] (- d (/ scroll-width 2)))
           (center-right [d] (+ d (/ scroll-width 2)))]
     (case position
       ;;
       (#{:top :left}
        #{:bottom :left})
       (update data :position/right move-right)
       ;;
       (#{:top :center}
        #{:bottom :center}
        #{:right :center})
       (->
        data
        (update :position/left center-left)
        (update :position/right center-right))
       ;;
       (#{:top :right}
        #{:bottom :right}
        #{:left :center})
       (update data :position/left move-left)
       :else data))))

; (defn update-dropdown-size
;   [data]
;   (assoc
;    data
;     :popup-width (- (:position/right data) (:position/left data))
;     :popup-height (- (:position/bottom data) (:position/top data))))

(defn best-candidate
  "For given number of candidates, this function will find candidate
  that has least overflow, that is candidate that has most visibility"
  [candidates]
  (let [[{:keys [overflow/left
                 overflow/right
                 overflow/top
                 overflow/bottom
                 window-height
                 window-width]
          :as candidate}]
        (reverse
         (sort-by
          (fn [{:keys [overflow/left
                       overflow/right
                       overflow/bottom
                       overflow/top]}]
            (+ left right bottom top))
          candidates))]
    (->
     candidate
     #_(cond-> candidate
         (neg? top) (assoc :position/top 3)
         (neg? bottom) (assoc :position/bottom (- window-height 5))
         (neg? left) (assoc :position/left 3)
         (neg? right) (assoc :position/right (- window-width 3)))
     (adjust-scroll-width 15)
     #_update-dropdown-size)))

(defn ^:no-doc padding-data
  [el]
  (when el
    (letfn [(px->int [x]
              (when x (js/parseInt (re-find #"\d+" x))))]
      (when-let [padding (util/css-attributes
                          el
                          :padding-left :padding-right
                          :padding-top :padding-bottom)]
        (reduce
         (fn [r k]
           (update r k px->int))
         padding
         (keys padding))))))

(defn compute-container-props
  "For given popup area and popup element function will return
  candidate that is most suitable for showing popup"
  ([target el] (compute-container-props target el default-preference))
  ([target el preference]
   (let [preference (or preference default-preference)]
     (when-let [props (computation-props target el)]
       (let [candidates (map
                         #(compute-candidate (assoc props :position %))
                         preference)]
         (when (empty? candidates)
           (throw
            (ex-info
             "Coulnd't find suitable candidate"
             {:candidates candidates
              :target @target
              :element @el
              :preference preference})))
         (merge
          (if-let [prefered-candidate (some ok-candidate? candidates)]
            prefered-candidate
             ;; There was no full size candidate
            (best-candidate candidates))
          (padding-data el)))))))

(defhook use-focusable-items
  "Should be used with popups that can focus some option
  or elmenent. Like dropdowns or multiselects
  
  Hook will return `ref-fn` and `focus-option` function.
  
  `ref-fn` accepts option and binds that option element
  with ref to DOM element in internal state.
  
  When focus-option function is called for some option
  it will use .scrollIntoView to focus that option, by
  pulling out ref from internal memory using input option"
  ([] (use-focusable-items false))
  ([direction]
   (let [oels (hooks/use-ref {})]
     (letfn [(focus-option [selected]
               (when selected
                 (when-let [option-element (get @oels selected)]
                   (.scrollIntoView option-element direction))))
             (ref-fn [option] #(swap! oels assoc option %))]
       [ref-fn focus-option]))))

(defhook use-outside-action
  "Hook accepts area and optionally popup element
  and handler that will be called when outside click
  or scroll actions are made."
  ([area handler]
   (use-outside-action area area handler))
  ([area popup handler]
   (use-outside-action true area popup handler))
  ([opened area popup handler]
   (hooks/use-effect
     [opened]
     (when opened
       (letfn [(handle-outside-click [e]
                 (cond
                   (and (some? area) (some? @area) (.contains @area (.-target e))) nil
                   ;; When clicke on popup do nothing
                   (and (some? popup) (some? @popup) (.contains @popup (.-target e))) nil
                   ;; Else call outside action handler
                   (and (some? area) (some? @area) (some? popup) (some? @popup))
                   (do
                     #_(t/log!
                        {:id ::outside-click
                         :level :debug
                         :data {:element e
                                :area @area
                                :popup @popup}})
                     (handler e))
                   ;;
                   :else nil))
               (handle-outside-scroll [e]

                 (cond
                   (and (some? popup) (some? @popup) (.contains @popup (.-target e))) nil
                   ;; Else call outside action handler
                   (and (some? popup) (some? @popup))
                   (do
                     #_(t/log!
                        {:id ::outside-scroll
                         :level :debug
                         :data {:element e
                                :area @area
                                :popup @popup}})
                     (handler e))
                   ;;
                   :else nil))]
         (async/go
           (async/<! (async/timeout 100))
           (.addEventListener js/document "touchmove" handle-outside-click)
           (.addEventListener js/document "mousedown" handle-outside-click)
           (.addEventListener js/document "wheel" handle-outside-scroll))
         (fn []
           (.removeEventListener js/document "touchmove" handle-outside-click)
           (.removeEventListener js/document "mousedown" handle-outside-click)
           (.removeEventListener js/document "wheel" handle-outside-scroll)))))))

(defnc Container
  "Component that in most cases is instantiated only once. Somewhere
  near react mounted component.

  This component is target for popups to mount. So Element function
  will look for this component that will provide `*container*` context
  and mount popup in Container."
  [props]
  (let [container (hooks/use-ref nil)]
    (provider
     {:context *container*
      :value container}
     (<>
      (c/children props)
      (d/div
       {:id "toddler-popups"
        :ref #(reset! container %)})))))

(defn wrap-container
  "Wrapper that will use Container component to
  render children if user is authorized"
  ([component]
   (fnc Container [props]
     ($ Container ($ component {& props})))))

(defnc Area
  "Component that will generate div element around children
  that will be provided as `*area-element*` context. 

  It is element that will listen for user events and open
  popup"
  {:wrap [forward-ref]}
  [props _ref]
  (let [area (hooks/use-ref nil)
        area' (or _ref area)]
    (d/div
     {:ref #(reset! area' %)
      & props}
     (provider
      {:context *area-element*
       :value @area'}
      (c/children props)))))

(defnc Element
  "Creates fixed positioned div element that is positioned
  based on preference to *area* context element.
  
  Popup size will adjust to content that is inside of 
  this element. Props:
    
   * :preference - position preference in form of sequence with
                   set options. I.E. `[#{:bottom :left} #{:top :left}]`
   * :style      - style that will override Element div
   * :offset     - distance between Area and Element 
   * :on-change  - when popup position changes this will be called"
  {:helix/features {:fast-refresh true}
   :wrap [forward-ref]}
  [{:keys [preference style offset onChange on-change]
    :or {preference default-preference
         offset 5}
    :as props} ref]
  (let [[{:keys [position/top position/left position]
          :as computed
          :or {left -10000
               top -10000}}
         set-state!] (hooks/use-state nil)
        ;;
        on-change (or on-change onChange)
        ;;
        el (hooks/use-ref nil)
        _el (or ref el)
        resize-observer (hooks/use-ref nil)
        mutation-observer (hooks/use-ref nil)
        animation-frame-id (hooks/use-ref nil)
        target-position (hooks/use-ref nil)
        mounted? (hooks/use-ref true)
        target (hooks/use-context *area-element*)
        container-node (hooks/use-context *container*)
        cache (hooks/use-ref {:preference preference :offset offset})]
    (letfn [(recalculate []
              (let [{:keys [offset preference]} @cache]
                (binding [*offset* offset]
                  (let [_computed (compute-container-props target @_el preference)]
                    (when (not= computed _computed)
                      (set-state! _computed)
                      (when (ifn? on-change) (on-change _computed)))))))]
      ;; When offset or preference change... call recalcualte
      (hooks/use-layout-effect
        [offset preference]
        (reset! cache {:offset offset :preference preference})
        (recalculate))
      ;; Keep track of target
      (letfn [(on-target-change []
                (let [current (util/window-element-position target)]
                  (when (not= current @target-position)
                    (reset! target-position current)
                    (recalculate)))
                (when @mounted?
                  (js/requestAnimationFrame on-target-change)))]
        (hooks/use-effect
          :always
          (when (and (some? @_el) (not @animation-frame-id))
            (let [af (js/requestAnimationFrame on-target-change)]
              (reset! animation-frame-id af)))))
      ;; Keep track of popup element resizing
      (hooks/use-effect
        :always
        (when (and (some? @_el) (not @resize-observer))
          (reset! resize-observer (js/ResizeObserver. recalculate))
          (.observe @resize-observer @_el)
          (let [_computed (compute-container-props target @_el preference)]
            (when (not= computed _computed)
              (set-state! _computed)
              (when (ifn? on-change) (on-change _computed)))))
        (when (and (some? target)
                   (some? @_el)
                   (not @mutation-observer))
          (reset! mutation-observer (js/MutationObserver. recalculate))
          (.observe @mutation-observer
                    target
                    #js {:childList true
                         :attributes true
                         :subtree true
                         :attributeFilter #js ["class" "style"]})))
      ;; Track user move events
      (hooks/use-effect
        :once
        (.addEventListener js/document "wheel" recalculate #js {:pasive false})
        (.addEventListener js/document "touchmove" recalculate #js {:pasive false})
        ;; Cleanup
        (fn []
          (when @resize-observer (.disconnect @resize-observer))
          (when @mutation-observer (.disconnect @resize-observer))
          (reset! mounted? false)
          (.removeEventListener js/document "wheel" recalculate)
          (.removeEventListener js/document "touchmove" recalculate))))
    (when (nil? container-node)
      (.error js/console "Popup doesn't know where to render. Specify popup container. I.E. instantiate toddler.popup/Container"))
    (rdom/createPortal
     (provider
      {:context *position*
       :value position}
      (provider
       {:context *container-dimensions*
        :value computed}
       (let [style (cond->
                    (merge
                     style
                     {:top top :left left
                      :position "fixed"
                      :box-sizing "border-box"
                      :zIndex "1000"
                      :visibility (if computed "visible" "hidden")})
                     ;;
                     (not (pos? (:popup-height computed)))
                     (assoc :visibility "hidden"
                            :opacity "0"))]
         (d/div
          {:ref #(reset! _el %)
           :style style
           & (select-keys props [:class :className])}
          (c/children props)))))
     @container-node)))
