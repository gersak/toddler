(ns toddler.elements.popup
  (:require
    clojure.string
    [cljs-bean.core :refer [->js]]
    ["react" :as react]
    ["react-dom" :as rdom]
    [helix.core 
     :refer [defnc $ provider <>
             defhook create-context]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.children :as c]
    [vura.core :refer [round-number]]
    [helix.styled-components :refer [defstyled --themed]]
    [toddler.elements.scroll :refer [SimpleBar]]
    [toddler.util :as util]))

(.log js/console "Loading toddler.elements.popup")

(def ^:dynamic ^js *area-element* (create-context))
(def ^:dynamic ^js *dimensions* (create-context))
(def ^:dynamic ^js *container* (create-context))
(def ^:dynamic ^js *outside-action-channel* (create-context))
(def ^:dynamic ^js *area-position* (create-context))


(def default-preference
  [#{:bottom :left} 
   #{:bottom :right}
   #{:top :left}
   #{:top :right}
   #{:bottom :center}
   #{:top :center}
   #{:left :center}
   #{:right :center}])

(def central-preference
  [#{:bottom :center} 
   #{:top :center}
   #{:bottom :left}
   #{:bottom :right}
   #{:top :left}
   #{:top :right}
   #{:left :center}
   #{:right :center}])

(def left-preference
  [#{:bottom :left} 
   #{:top :left}
   #{:bottom :center}
   #{:top :center}
   #{:bottom :right}
   #{:top :right}
   #{:left :center}
   #{:right :center}])

(def right-preference
  [#{:bottom :right} 
   #{:top :right}
   #{:bottom :center}
   #{:top :center}
   #{:top :left}
   #{:bottom :left}
   #{:left :center}
   #{:right :center}])

(def cross-preference
  [#{:bottom :center}
   #{:right :center}
   #{:left :center}
   #{:top :center}])


(def ^:dynamic ^js *offset* 6)

(defmulti compute-candidate (fn [{:keys [position]}] position))

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
           window-width
           window-height
           right
           bottom]
    :as data}]
  (let [dl (- window-width right popup-width)
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
           window-width
           top
           right]
    :as data}]
  (let [dt (- top popup-height *offset*)
        dl (- window-width right popup-width)] 
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


(defn computation-props [target el]
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
  [{:keys [overflow/top
           overflow/bottom
           overflow/left
           overflow/right]
    :as candidate}]
  (when (not-any? neg? [top bottom left right])
    candidate))


(defn adjust-scroll-width
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


(defn update-dropdown-size
  [data]
  (assoc  data
         :popup-width (- (:position/right data) (:position/left data))
         :popup-height (- (:position/bottom data) (:position/top data))))

(defn best-candidate
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
      (cond-> candidate
        (neg? top) (assoc :position/top 3)
        (neg? bottom) (assoc :position/bottom (- window-height 5))
        (neg? left) (assoc :position/left 3)
        (neg? right) (assoc :position/right (- window-width 3)))
      (adjust-scroll-width 15)
      update-dropdown-size)))

(defn padding-data [el]
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
  ([target el] (compute-container-props target el default-preference))
  ([target el preference]
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
         (padding-data el))))))

;; DROPDOWN CONTAINER
; (defstyled dropdown-container
;   "div"
;   {:max-height 600
;    :min-width 50
;    :padding "2px 0"
;    :overflow "auto"
;    ".dropdown-content"
;    {:display "flex"
;     :flex-direction "column"}}
;   --themed)


(defhook use-focusable-items
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
  [opened area popup handler]
  (hooks/use-effect
    [opened]
    (when opened
      (letfn [(handle [e] 
                (cond
                  (and (some? @area) (.contains @area (.-target e))) nil
                  ;; When clicke on popup do nothing
                  (and (some? @popup) (.contains @popup (.-target e))) nil
                  ;; Else call outside action handler
                  (and (some? @area) (some? @popup))
                  (handler e)
                  ;;
                  :else nil))]
        (.addEventListener js/document "click" handle)
        (.addEventListener js/document "wheel" handle)
        (fn []
          (.removeEventListener js/document "click" handle)
          (.removeEventListener js/document "wheel" handle))))))


(defnc Container
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

(defnc Area
  [{:keys [render className] 
    :or {render "div"} 
    :as props} ref]
  {:wrap [(react/forwardRef)]}
  (let [area (hooks/use-ref nil)
        area' (or ref area)]
    ($ render
       {:ref #(reset! area' %)
        :className className
        & props}
       (provider
         {:context *area-element*
          :value @area'} 
         (c/children props)))))


(defstyled element "div"
  {:display "flex"
   :justify-content "start"
   :flex-wrap "wrap"
   :border-radius 3
   :padding 7}
  --themed)

(.log js/console "Loaded toddler.elements.popup!")

(defnc Element
  [{:keys [preference style className offset wrapper onChange items]
    :or {preference default-preference
         offset 5
         wrapper element}
    :as props} ref]
  {:helix/features {:fast-refresh true}
   :wrap [(react/forwardRef)]}
  (let [[{:keys [position/top position/left
                 popup-width
                 popup-height
                 padding-top
                 padding-left
                 padding-right
                 padding-bottom]
          :as computed
          :or {left -10000
               top -10000
               padding-left 0
               padding-right 0
               padding-bottom 0
               padding-top 0}}
         set-state!] (hooks/use-state nil)
        ;;
        dummy (hooks/use-ref nil)
        el (hooks/use-ref nil)
        target (hooks/use-context *area-element*)
        container-node (hooks/use-context *container*)]
    (hooks/use-effect
      [items]
      (binding [*offset* offset] 
        (let [computed (compute-container-props target @dummy preference)]
          (set-state! computed)
          (when (ifn? onChange) (onChange computed)))))
    (when (nil? container-node)
      (.error js/console "Popup doesn't know where to render. Specify popup container. I.E. instantiate toddler.elements.popup/Container"))
    (rdom/createPortal
      (provider
        {:context *dimensions*
         :value computed}
        (<>
          ($ wrapper
             {:ref #(reset! dummy %)
              :className className
              :style (->js
                       (merge
                         style
                         {:top -10000 :left -10000 
                          :position "fixed"
                          :zIndex "1000"
                          :visibility "hidden"}))}
             ;; TODO - think about what happens when height is limited by css
             ;; or style properties. Than commented line doesn't work well
             ;; on the other hand with current line simplebar is always present
             ;; even for small popups.... this is not goood
             ; (if (and computed (not (ok-candidate? computed)))
             (c/children props))
          (when computed
            ($ wrapper
               {:ref #(reset! (or ref el) %)
                :className className
                :style (->js
                         (merge
                           style
                           {:top top :left left
                            :position "fixed"
                            :zIndex "1000"}))}
               ($ SimpleBar
                  {:style #js {:width (round-number (- popup-width padding-left padding-right) 1 :ceil)
                               :height (round-number (- popup-height padding-top padding-bottom) 1 :ceil)}}
                  (c/children props))))))
      @container-node)))




(comment
  (def els (array-seq (.getElementsByClassName js/document "tooltip-container")))
  (def el (first els)))
