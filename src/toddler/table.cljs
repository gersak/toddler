(ns toddler.table
  (:require
   [clojure.string :as str]
   goog.string
   [helix.dom :as d]
   [helix.core
    :refer [defhook defnc memo
            create-context $
            <> provider]]
   [helix.hooks :as hooks]
   [helix.children :as c]
   [toddler.i18n]
   [toddler.ui :as ui]
   [toddler.hooks :as toddler]
   [toddler.layout :as layout]))

(def ^:dynamic ^js *column* (create-context))
(def ^:dynamic ^js *entity* (create-context))
(def ^:dynamic ^js *columns* (create-context))
(def ^:dynamic ^js *actions* (create-context))
(def ^:dynamic ^js *row-record* (create-context))
(def ^:dynamic ^js *rows* (create-context))
(def ^:dynamic ^js *dispatch* (create-context))

(defhook use-columns [] (hooks/use-context *columns*))
(defhook use-column [] (hooks/use-context *column*))
(defhook use-rows [] (hooks/use-context *rows*))
(defhook use-row [] (hooks/use-context *row-record*))

(defhook use-dispatch [] (hooks/use-context *dispatch*))

(defnc NotImplemented
  []
  (let [field (use-column)]
    (.error js/console (str "Field not implemented\n%s" (pr-str field))))
  nil)

(defnc Cell
  {:wrap [(memo #(= (:column %1) (:column %2)))]}
  [{{:keys [style level]
     render :cell
     :as column} :column
    :or {render "div"} :as props}]
  (when (nil? render)
    (.error "Cell renderer not specified for culumn " (pr-str column)))
  (let [w (get style :width 100)]
    ;; Field dispatcher
    (d/div
     {:style (merge
              style
              {:display "flex"
               :flex (str w  \space 0 \space "auto")
               :position "relative"
               :min-width w
               :width w})
      :level level
      & (dissoc props :style :level :render :column)}
     (provider
      {:context *column*
       :value column}
      ($ render)))))

(defhook use-table-width
  []
  (let [columns (use-columns)]
    (reduce + 0 (map #(get-in % [:style :width] 100) columns))))

(defnc FRow
  {:wrap [(ui/forward-ref)]}
  [props _ref]
  (let [min-width (use-table-width)]
    (d/div
     {:style {:display "flex"
              :flexDirection "row"
              :justifyContent "flex-start"
               ; :justifyContent "space-around"
              :flex (str 1 \space 0 \space "auto")
              :minWidth min-width}
      :level (:level props)
      & (cond->
         (dissoc props :style :level :data)
          _ref (assoc :ref _ref))}
     (c/children props))))

(defnc Row
  {:wrap [(ui/forward-ref)
          (memo
           (fn [{idx1 :idx data1 :data} {idx2 :idx data2 :data}]
             (and
              (= idx1 idx2)
              (= data1 data2))))]}
  [{data :data
    :keys [className idx render class]
    :as props
    :or {render FRow}} _ref]
  (let [columns (use-columns)]
    (provider
     {:context *row-record*
      :value (assoc data :idx idx)}
     (<>
      ($ render
         {:ref _ref
          :key idx
          :class (str/join
                  " "
                  (cond-> [(if (even? idx) " even" " odd")]
                    (string? class) (conj class)
                    className (conj className)
                    (sequential? class) (into class)))
          & (dissoc props :class :className)}
         (map
          (fn [{:keys [attribute cursor] :as column}]
            (let [_key (or
                        (if (keyword? cursor) cursor
                            (when (not-empty cursor)
                              (str/join "->" (map name cursor))))
                        attribute)]
              ($ ui/table-cell
                 {:key _key
                  :column column})))
          (remove :hidden columns)))
      (c/children props)))))

(defn init-pagination
  "Given initial page size, page and total count
  function returns initial props for pagination."
  [{{:keys [page page-size total-count options]
     :or {page 0
          page-size 20
          total-count 0
          options [10 20 50 100]}
     :as pagination} :pagination
    :as props}]
  (if (empty? pagination) props
      (assoc props :pagination
             {:page page
              :page-size page-size
              :total-count total-count
              :options options
              :page-count (.ceil js/Math (/ total-count page-size))
              :next? (<= (inc page) (.ceil js/Math (/ total-count page-size)))
              :previous? (>= (inc page) 1)})))

; (defnc Pagination
;   [{:keys [className]}]
;   (let [{:keys [page page-size
;                 next? previous? options
;                 page-count total-count]
;          :as pagination} (use-pagination)
;         dispatch (use-dispatch)
;         set-pagination! (hooks/use-memo
;                           [dispatch]
;                           (fn [v] 
;                             (dispatch 
;                               {:type :pagination/update
;                                :pagination v})))
;         rows (use-rows)]
;     (d/div
;       {:class className}
;       (when (and pagination (pos? rows) (> total-count (count rows)))
;         (d/button
;           {:onClick #(set-pagination! {:page 0})
;            :className "start"
;            :disabled (not previous?)}
;           ($ icon/paginationFarPrevious))
;         (d/button
;           {:onClick #(set-pagination! {:page (dec page)})
;            :className "previous"
;            :disabled (not previous?)}
;           ($ icon/paginationPrevious))
;         (d/button
;           {:onClick #(set-pagination! {:page (inc page)})
;            :className "next"
;            :disabled (not next?)}
;           ($ icon/paginationNext))
;         (d/button
;           {:onClick #(set-pagination! {:page (dec page-count)})
;            :className "end"
;            :disabled (not next?)}
;           ($ icon/paginationFarNext))
;         (d/span 
;           (goog.string/format
;             "Showing %d - %d of %d results" 
;             (* page page-size)
;             (+ (* page page-size) (count rows))
;             total-count))
;         (d/select
;           {:value page-size
;            :className "view-size"
;            :onChange (fn [e] (set-pagination! {:page-size (js/Number (.. e -target -value))}))}
;           (map
;             (fn [option]
;               (d/option
;                 {:key option
;                  :value option}
;                 (str "Show " option)))
;             (or options (range 10 60 10))))
;         (d/span (str "| Page: "))
;         ($ ui/idle-input
;            {:placeholder "?"
;             :className "pag"
;             :spell-check false
;             :auto-complete "off"
;             :type "number"
;             :value (inc page)
;             :onChange (fn [page] 
;                         (let [page (if page page 0)
;                               np (min (max 0 (dec page)) page-count)]
;                           (set-pagination! {:page np})))})))))

(defhook use-cell-state
  [el]
  (let [{:keys [idx] :as row} (hooks/use-context *row-record*)
        {k :cursor} el
        k (hooks/use-memo
            [k]
            (if (sequential? k) k
                [k]))
        dispatch (use-dispatch)
        value (get-in row k)
        set-value! (hooks/use-memo
                     [idx dispatch]
                     (fn [value]
                       (when dispatch
                         (dispatch
                          {:type :table.element/change
                           :idx idx
                           :column el
                           :value value}))))
        select-value! (hooks/use-memo
                        [idx dispatch]
                        (fn [value]
                          (when dispatch
                            (dispatch
                             {:type :table.element/select
                              :idx idx
                              :column el
                              :value value}))))]
    [value set-value! select-value!]))

;;;;;;;;;;;;;;;;
;;   HEADERS  ;;
;;;;;;;;;;;;;;;;
(defnc ColumnNameElement
  [{{column-name :label
     :keys [order]
     :as column}
    :column}]
  (let [dispatch (use-dispatch)
        translate (toddler/use-translate)]
    (d/div
     {:className "name"
      :onClick (fn []
                 (when dispatch
                   (dispatch
                    {:type :table.column/order
                     :column column
                     :value
                     (case order
                       nil :desc
                         ;;
                       :desc :asc
                         ;;
                       nil)})))}
     (translate column-name))))

;; Styled headers
(defn column-default-style
  [{{:keys [width]
     :or {width 100}
     :as style} :style
    type :type :as column}]
  (assoc column :style
         (merge
          style
          {:display "flex"
           :flex (str width \space 0 \space "auto")
           :minWidth width
           :width width}
          (when (every? empty? ((juxt :justifyContent :alignItems) style))
            (case type
              ("boolean" "uuid") {:justifyContent "center"
                                  :alignItems "flex-start"}
              {:justifyContent "flex-start"
               :alignItems "flex-start"})))))

(defhook use-table-defaults
  [{:keys [columns] :as props}]
  (hooks/use-memo
    [columns]
    (->
     props
     (update :columns
             (fn [columns]
               (vec
                (map-indexed
                 (fn [idx c]
                   (assoc (column-default-style c)
                     :idx idx))
                 columns)))))))

(defnc Header
  {:wrap [(ui/forward-ref)]}
  [{:keys [className class]} _ref]
  (let [{container-width :width} (layout/use-container-dimensions)
        table-width (use-table-width)
        rows (use-rows)
        style {:minWidth table-width}
        columns (use-columns)]
    (when (some :header columns)
      ($ ui/simplebar
         {:key :thead/simplebar
          :ref _ref
          :className (str/join
                      " "
                      (cond-> ["thead"]
                        className (conj className)
                        (string? class) (conj class)
                        (sequential? class) (into class)))
          :style (cond->
                  {:width container-width
                   :maxHeight 500
                   :padding 0}
                   (empty? rows) (assoc :visible "hidden"))}
         (d/div
          {:key :thead/row
           :style style
           :className "trow"}
          (map
           (fn [{a :attribute
                 idx :idx
                 render :header
                 style :style
                 p :cursor
                 :as column}]
             (let [w (get style :width 100)]
               (d/div
                {:key idx
                 :style (merge style
                               {:display "flex"
                                :flex (str w  \space 0 \space "auto")
                                :min-width w
                                :width w})}
                (when render
                  ($ render
                     {:key (if (nil? p) a
                               (if (keyword? p) p
                                   (str/join "->" (map name (not-empty p)))))
                      :className "th"
                      :column column})))))
           (remove :hidden columns)))))))

(defnc Body
  {:wrap [(ui/forward-ref)]}
  [{:keys [class className]} _ref]
  (let [{container-width :width
         container-height :height} (layout/use-container-dimensions)
        rows (use-rows)
        table-width (use-table-width)
        style {:minWidth table-width
               :minHeight container-height}
        className (str/join " "
                            (cond-> ["tbody"]
                              className (conj className)
                              (string? class) (conj class)
                              (sequential? class) (into class)))]
    (when (and container-width container-height)
      ($ ui/simplebar
         {:key :tbody/simplebar
          :ref _ref
          :className className
          :style {:width container-width
                  :maxHeight container-height}}
         (d/div
          {:key :tbody
           :style style}
          (map-indexed
           (fn [idx row]
             ($ ui/table-row
                {:key (or
                       (:euuid row)
                       idx)
                 :idx idx
                 :className "trow"
                 :data row}))
           rows))))))

(defnc TableProvider
  [{:keys [dispatch rows]
    :as props}]
  (let [{:keys [columns]} (use-table-defaults props)]
    (provider
     {:context *dispatch*
      :value dispatch}
     (provider
      {:context *columns*
       :value columns}
      (provider
       {:context *rows*
        :value rows}
       (c/children props))))))
