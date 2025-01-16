(ns toddler.util
  (:require-macros [toddler.util :refer [mlf]])
  (:require
   goog.object
   clojure.string
   [goog.string :refer [format]]
   [clojure.core.async :as async]))

(set! *warn-on-infer* true)

(defn ml
  [& lines]
  (clojure.string/join "\n" lines))

(defn deep-merge
  "Recursively merges maps."
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn get-font-style [node]
  (when node
    (let [style (.getComputedStyle js/window node)]
      {:font-size (.-fontSize style)
       ;; BUG - This breaks input style copying and defaults font to Times New Roman
       ; :font-family (.-fontFamily style)
       :font-weight (.-fontWeight style)
       :letter-spacing (.-letterSpacing style)
       :text-transform (.-textTransform style)})))

(defn mouse-relative-coordinates
  "Function returns coordinates from in form [x,y] that are relative
   to center of element"
  [position element-frame]
  (let [[dx dy] (map - position (take 2 element-frame))
        [xc yc] (map #(/ % 2) (drop 2 element-frame))]
    [(- dx xc) (- yc dy)]))

(defn calculate-view-frame
  "Calculates view-frame based on input coordinates in regular
   Euclidian coordinate-system in respect to scale and component
   size where [x,y] are center coordinates of component."
  [[x y] component-size scale-factor]
  (let [view-frame (vec (map (partial * scale-factor) component-size))
        [dx dy] (map #(/ % 2) view-frame)
        xmin (- x dx)
        ymin (+ dy y)]
    (into [xmin ymin] view-frame)))

(defn calculate-frame-coordinates
  "Calculates frame start and end coordinates"
  [coordinates frame]
  (let [delta (map #(/ % 2) frame)]
    [(map - coordinates delta) (map + coordinates delta)]))

(defn calculate-translation
  "Calculates transition coordinates based on view-frame"
  [view-frame]
  (map + (take 2 view-frame) (map #(/ % 2) (drop 2 view-frame))))

;; Element functions

(defn get-element-rect
  "Returns rectangle in form
   [top left right bottom width height"
  [node]
  (when node
    (let [rect (.getBoundingClientRect node)
          width (.-width rect)
          height (.-height rect)
          left (.-left rect)
          top (.-top rect)
          bottom (.-bottom rect)
          right (.-right rect)]
      [top left right bottom width height])))

(defn get-css [node]
  (when node
    (.getComputedStyle js/window node)))

(defn select-attributes [css attributes]
  (assert (every? #(or (keyword? %) (string? %)) attributes) "Attributes not valid")
  (reduce
   (fn [r a]
     (assoc r a (goog.object/get css (name a))))
   nil
   attributes))

(defn css-attributes
  "Given DOM node and attributes function returns hash-map of pulled attributes
   from computed css in given DOM node."
  [node & attributes]
  (when node
    (->
     node
     get-css
     (select-attributes attributes))))

(defn css-attribute
  "Given DOM node and attribute in form of string or keyword, function
   returns value of that attribute if any."
  [node attribute]
  (-> node
      get-css
      (select-attributes [attribute])
      first
      val))

(defn css-position [node]
  (.-position (get-css node)))

(defn fixed? [node]
  (= "fixed" (css-attribute node :position)))

(defn relative? [node]
  (= "relative" (css-attribute node :position)))

(defn dom-parent [node]
  (when node (.-parentNode node)))

(defn get-dom-parents
  ([node] (get-dom-parents node []))
  ([node result]
   (if-let [p (dom-parent node)]
     (recur p (conj result p))
     result)))

(defn find-parent
  "Function returns first parent in js/document that satisfies predicate fn."
  [node predicate]
  (when node
    (if-let [p (dom-parent node)]
      (if-not (= js/document p)
        (if (predicate p) p
            (recur p predicate))
        nil)
      nil)))

(defn fixed-parent [node]
  (find-parent node #(= "fixed" (css-attribute % :position))))

(defn positioned-parent [node]
  (find-parent node #(not= "static" (css-attribute % :position))))

(defn overflow-parent [node]
  (find-parent
   node
   #(some
     #{"auto" "overlay" "scroll"}
     [(css-attribute % :overflow-x)
      (css-attribute % :overflow-y)])))

(defn overflow-parents [node]
  (loop [node node
         parents []]
    (if-let [p (overflow-parent node)]
      (recur p (conj parents p))
      parents)))

(defn in-fixed? [node]
  (boolean (fixed-parent node)))

(defn dom-dimensions [node]
  (when node
    (let [rect (.getBoundingClientRect node)]
      [(+ (.-left rect) (.-scrollX js/window))
       (+ (.-top rect) (.-scrollY js/window))
       (.-width rect)
       (.-height rect)])))

(defn dom-element-center
  "Function returns [x,y] center position of DOM element"
  [node]
  (let [[l t w h] (dom-dimensions node)]
    [(+ l (/ w 2)) (+ t (/ h 2))]))

(defn dom-element-position
  "Returns dom rectangle of current element in respect to first 
   positioned parent. Should be used for absolute positioning."
  ([node]
   (when node
     (let [rect (get-element-rect node)]
       (if-let [offset-parent (positioned-parent node)]
         (let [[ptop pleft] (get-element-rect offset-parent)
               [scroll-top scroll-left] [(.-scrollTop offset-parent) (.-scrollLeft offset-parent)]
               scroll-top (* -1 scroll-top)
               scroll-left (* -1 scroll-left)
               rect' (->
                      rect
                      (update 0 - ptop scroll-top)
                      (update 1 - pleft scroll-left)
                      (update 2 - pleft scroll-left)
                      (update 3 - ptop scroll-top))]
           rect')
         rect)))))

(defn window-element-position
  "Returns dom rectangle of current element in respect to first 
   positioned parent. Should be used for absolute positioning."
  ([node]
   (when node
     (let [rect (get-element-rect node)
           [scroll-top scroll-left] [(.-scrollX js/window) (.-scrollY js/window)]
           scroll-top (* -1 scroll-top)
           scroll-left (* -1 scroll-left)]
       (->
        rect
        (update 0 - scroll-top)
        (update 1 - scroll-left)
        (update 2 - scroll-left)
        (update 3 - scroll-top))))))

(defn mouse-position
  "Returns mouse position from event"
  [e]
  [(.-pageX e) (.-pageY e)])

(defn not-in-area?
  "Returns true if position is in area"
  [[l t w h] [x y]]
  (or
   (<= x l) (<= y t)
   (>= x (+ w l)) (>= y (+ t h))))

(def in-area? (complement not-in-area?))

(defn check-drag-leave
  "Checks if mouse has really left component and all of its children"
  [el e]
  (let [position (mouse-position e)
        area (dom-dimensions el)]
    (not-in-area? area position)))

;; Caret position
(defn selection-start [e]
  (.. e -target -selectionStart))

(defn selection-end [e]
  (.. e -target -selectionEnd))

(defn selection [e]
  ((juxt selection-start selection-end) e))

(defn selection? [e]
  (apply not= (selection e)))

(defn partition-selection [e]
  (let [value (.. e -target -value)
        [s e] (selection e)]
    [(when (not= 0 s)
       (apply str (take s value)))
     (when (not= s e)
       (apply str
              (take
               (- e s)
               (drop s value))))
     (when (not= e (count value))
       (apply str (drop e value)))]))

(defn make-recuring-service
  ([period f]
   (assert (and (number? period) (pos? period)) "Timeout period should be positive number.")
   (assert (fn? f) "Function not provided. No point if no action is taken on idle timeout.")
   (let [close-channel (async/chan)]
     ;; When some change happend
     (async/go-loop [v true]
       (if (nil? v)
         :CLOSED
         ;; If not nil new value received and now idle handling should begin
         (do
           (f)
           (let [[value _] (async/alts!
                            [close-channel
                             (async/go (async/<! (async/timeout period)) ::TIMEOUT)])]
             (recur value)))))
     close-channel)))

(defn bounding-client-rect
  ([node]
   (let [rect (.getBoundingClientRect node)]
     {:x (.-x rect)
      :y (.-y rect)
      :height (.-height rect)
      :width (.-width rect)
      :top (.-top rect)
      :left (.-left rect)
      :right (.-right rect)
      :bottom (.-bottom rect)})))

(defn make-idle-service
  ([period f]
   (assert (and (number? period) (pos? period)) "Timeout period should be positive number.")
   (assert (fn? f) "Function not provided. No point if no action is taken on idle timeout.")
   (let [idle-channel (async/chan)]
     ;; When some change happend
     (async/go-loop [v (async/<! idle-channel)]
       (if (nil? v)
         :IDLED
         ;; If not nil new value received and now idle handling should begin
         (let [aggregated-values
               (loop [[value _]
                      (async/alts!
                       [idle-channel (async/go (async/<! (async/timeout period))
                                               ::TIMEOUT)])
                      r [v]]
                 (if (or
                      (= ::TIMEOUT value)
                      (nil? value))
                   (conj r value)
                   (recur (async/alts! [idle-channel (async/go (async/<! (async/timeout period)) ::TIMEOUT)]) (conj r value))))]
           ;; Apply function and if needed recur
           (f aggregated-values)
           (if (nil? (last aggregated-values))
             nil
             (recur (async/<! idle-channel))))))
     idle-channel)))

(defn index-by [k coll]
  (reduce
   (fn [r item] (assoc r (get item k) item))
   nil
   coll))

(defn sync-list
  ([items item] (sync-list items item :euuid))
  ([items item pred]
   (let [target (pred item)
         idx (.indexOf (mapv pred items) target)]
     (if (neg? idx)
       (conj items item)
       (assoc (vec items) idx item)))))

(defn modify-item
  "Modify items in sequence of items by mergeing input item with
  present item. Two items are matched by predicate."
  ([items item]
   (modify-item items item :euuid))
  ([items item pred]
   (reduce
    (fn [r item']
      (conj r
            (if (= (pred item) (pred item'))
              (merge item' item)
              item')))
    []
    items)))

(defn add-to-list
  "Adds item to items collection and sorts that collection afterwards..."
  ([items item] (add-to-list items item :name))
  ([items item _comparator]
   (vec (sort-by _comparator (conj items item)))))

(defn remove-from-list
  "Removes item from items by matching item with pred"
  ([items item] (remove-from-list items item :euuid))
  ([items item pred]
   (let [x (pred item)]
     (vec
      (remove
       #(= (pred %) x)
       items)))))

(defn wrap [o]
  (reify
    ILookup
    (-lookup [_ k]
      (goog.object/get o (name k)))
    (-lookup [_ k not-found]
      (goog.object/get o (name k) not-found))))
