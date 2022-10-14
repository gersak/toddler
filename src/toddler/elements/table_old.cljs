(ns toddler.elements.table
  (:require 
    ["react" :as react]
    [cljs-bean.core :refer [->clj]]
    [helix.core :refer [defnc]]
    [helix.hooks :as hooks]
    [helix.children :as c]
    [helix.dom :as d]))


(defn alignment-style
  ([align]
   (when align
     (cond-> nil
       (align :right) (assoc :justifyContent "flex-end")
       (align :left) (assoc :justifyContent "flex-start")
       (align :top) (assoc :alignItems "flex-start")
       (align :bottom) (assoc :alignItems "flex-end")
       (align :center) (cond->
                         (or (= align #{:top :center})
                             (= align #{:bottom :center}))
                         (assoc :justifyContent "center")
                         ;;
                         (or (= align #{:left :center})
                             (= align #{:right :center}))
                         (assoc :alignItems "center")
                         ;;
                         (= align #{:center}) 
                         (assoc :alignItems "center"
                                :justifyContent "center"))))))


(defnc AHeader
  [{:keys [className]
    {:keys [width]} :column
    :or {width 150}
    :as props}]
  ; {:wrap [(react/forwardRef)]}
  (d/div
    {:className className
     :style {:display "inline-block"
             :box-sizing "border-box"
             :width width}}
    (c/children props)))


(defnc ACell
  [{:keys [cell/column className] :as props}]
  (d/div
    {:className className
     :style {:display "inline-block"
             :box-sizing "border-box"
             :width (:width column)}
     :level (:level props)}
    (d/div 
      {:style 
       (merge 
         (alignment-style (:align column))
         {:display "flex"
          ;; TODO - this was enabled to properly display structure table
          ;; in Explorer... Try to avoid this
          ; :height "100%"
          })} 
      (c/children props))))


(defnc ARow
  [{:keys [columns className] :as props}]
  ; {:wrap [(react/forwardRef)]}
  (d/div
    {:className className
     :style {:display "flex"
             :width (apply  +  (map #(get % :width 150) columns))}
     :level (:level props)}
    (c/children props)))



(defnc HRow
  [{:keys [columns className] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [min-width (reduce + 0 (map #(get-in % [:style :width]) columns))] 
    (d/div
      {:class className
       :style {:display "flex"
               :flexDirection "row"
               :justifyContent "flex-start"
               :flex (str 1 \space 0 \space "auto")
               :minWidth min-width}
       :level (:level props)
       & (cond-> nil
           (some? _ref) (assoc :ref _ref))}
      (c/children props))))



(def ^:dynamic *min-width* 100)

(defnc FRow
  [{:keys [row/cells className] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [min-width (reduce + 0 (map #(get-in % [:cell/column :style :width]) cells))] 
    (d/div
      {:class className
       :style {:display "flex"
               :flexDirection "row"
               :justifyContent "flex-start"
               :flex (str 1 \space 0 \space "auto")
               :minWidth min-width}
       :level (:level props)
       & (cond-> nil
           (some? _ref) (assoc :ref _ref))}
      (c/children props))))

(defnc FHeader
  [{:keys [className]
    {:keys [align disabled style]} :column
    :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [w (get style :width *min-width*)] 
    (d/div
      {:class className
       :disabled disabled
       :style (merge 
                (->clj style)
                (alignment-style align)
                {:display "flex"
                 :flex (str w \space 0 \space "auto")
                 :minWidth w
                 :width w})
       & (cond-> nil
           (some? _ref) (assoc :ref _ref))}
      (c/children props))))


(defnc FCell
  [{:keys [cell/column className cell/disabled] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [{:keys [align style]
         :or {style {:minWidth *min-width*
                     :width *min-width*}}} column
        w (get style :width *min-width*)] 
    (d/div
      {:class className
       :disabled disabled
       :style (merge 
                (->clj style)
                (alignment-style align)
                {:display "flex"
                 :flex (str w  \space 0 \space "auto")
                 :min-width w
                 :width w})
       :level (:level props)
       & (cond-> nil
           (some? _ref) (assoc :ref _ref))}
      (c/children props))))


(defn use-table 
  [{:keys [data columns]}]
  (let [rows (hooks/use-memo
               [columns data]
               (vec
                 (keep-indexed
                   (fn [idx row]
                     (hash-map
                       :row/data row
                       :row/index idx
                       :row/cells (reduce
                                    (fn [cells column]
                                      (conj cells 
                                        (hash-map
                                          :cell/id (:id column)
                                          :cell/data (if-let [a (:accessor column)] 
                                                       (a row)
                                                       nil)
                                          :cell/row idx
                                          :cell/read-only (:read-only column)
                                          :cell/disabled (:disabled column)
                                          :cell/column column)))
                                    []
                                    columns)))
                   data)))]
    {:table/rows rows
     :table/columns columns}))


(defn pagination-reducer
  "Function accepts current pagination state and pagination
  next-pagination state and computes other props usefull for pagination.
  Returns map with keys:
  * page - current page
  * page-size - number of results per page
  * total-count - total number of results
  * page-count - total number of pages
  * next? - has next page?
  * previous? - has previous page?"
  [pagination {:keys [page page-size total-count]}]
  (cond-> pagination 
    (some? page)
    ;;
    (assoc 
      :page (max (min page (dec (:page-count pagination))) 0)
      :next? (< (inc page) (:page-count pagination))
      :previous? (> page 0))
    ;;
    (some? page-size)
    (assoc 
      :page (max 
              (.floor js/Math (* (:page pagination) (/ (:page-size pagination) page-size)))
              0)
      :page-size page-size
      :page-count (.ceil js/Math (/ (:page-size total-count) page-size)))
    ;;
    (some? total-count)
    (as-> p
      (let [page-count' (.ceil js/Math (/ total-count (:page-size p)))] 
        (if (pos? (:total-count p)) 
          (let [page' (* (:page p) (.floor js/Math (/ total-count (:total-count pagination))))] 
            (assoc p
                   :page page' 
                   :page-count page-count'
                   :next? (< (inc page') page-count')
                   :previous? (> page' 0)
                   :total-count total-count))
          (assoc p
                 :page 0 
                 :next? (< (inc page) page-count')
                 :previous? false
                 :page-count page-count'
                 :total-count total-count))))))


(defn init-pagination
  "Given initial page size, page and total count
  function returns initial props for pagination."
  [{:keys [page page-size total-count]
    :or {page 0 
         page-size 20 
         total-count 0}}]
  {:page page
   :page-size page-size
   :total-count total-count
   :page-count (.ceil js/Math (/ total-count page-size))
   :next? (<= (inc page) (.ceil js/Math (/ total-count page-size)))
   :previous? (>= (inc page) 1)})


(defn use-pagination 
  [props]
  (let [[pagination pagination-change!] 
        (hooks/use-reducer pagination-reducer (init-pagination props))]
    [pagination pagination-change!]))
