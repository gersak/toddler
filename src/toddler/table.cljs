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
   [toddler.core :as toddler]
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
    (.error js/console "Cell renderer not specified for culumn " (pr-str column)))
  (let [w (or
           (get column :width)
           (get style :width 100))
        style (merge
               style
               {:display "flex"
                :flex (str w  \space 0 \space "auto")
                :position "relative"
                :min-width w
                :width w})]
    ;; Field dispatcher
    (d/div
     {:style style
      :level level
      & (dissoc props :style :width :level :render :column)}
     (provider
      {:context *column*
       :value column}
      ($ render)))))

(defhook use-table-width
  []
  (let [columns (use-columns)]
    (reduce + 0 (map #(get-in % [:style :width] (get % :width 100)) columns))))

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
         (dissoc props :style :level :data :width)
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

(defhook use-cell-state
  "Hook that will return vector of

  [value set-value! select-value!
  
  Value is value pulled from row based on :cursor position
  in column declaration. set-value! and select-value! will
  use dispatch from *dispatch* context to send events when
  value changes or value is selected.
  
  Use dispatch from your reducer to set *dispatch* context
  that this hook will use."
  ([]
   (let [column (use-column)]
     (use-cell-state column)))
  ([column]
   (let [{:keys [idx] :as row} (hooks/use-context *row-record*)
         {k :cursor} column
         k (hooks/use-memo
             [k]
             (if (sequential? k) k
                 [k]))
         dispatch (use-dispatch)
         value (hooks/use-memo
                 [row]
                 (get-in row k))
         set-value! (hooks/use-memo
                      [idx dispatch]
                      (fn [value]
                        (when dispatch
                          (dispatch
                           {:type :table.element/change
                            :idx idx
                            :column column
                            :value value}))))
         select-value! (hooks/use-memo
                         [idx dispatch]
                         (fn [value]
                           (when dispatch
                             (dispatch
                              {:type :table.element/select
                               :idx idx
                               :column column
                               :value value}))))]
     [value set-value! select-value!])))

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

(defn column-default-style
  [{style :style
    type :type :as column}]
  (let [width (get-in column [:style :width]
                      (get column :width 100))
        alignment (case (:align column)
                    (:center #{:top :center}) {:justifyContent "center"
                                               :alignItems "flex-start"}
                    (:right #{:top :right}) {:justifyContent "flex-end"
                                             :alignItems "flex-start"}
                    #{:center :left} {:justifyContent "flex-start"
                                      :alignItems "center"}
                    #{:center :right} {:justifyContent "flex-end"
                                       :alignItems "center"}
                    #{:center} {:justifyContent "center"
                                :alignItems "center"}
                    (:bottom  #{:bottom-left}) {:justifyContent "flex-start"
                                                :alignItems "flex-end"}
                    #{:bottom :center} {:justifyContent "center"
                                        :alignItems "flex-end"}
                    #{:bottom :right} {:justifyContent "flex-end"
                                       :alignItems "flex-end"}
                    {:justifyContent "flex-start"
                     :alignItems "flex-start"})]
    (assoc column :style
           (merge
            style
            {:display "flex"
             :flex (str width \space 0 \space "auto")
             :minWidth width
             :width width}
            alignment))))

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
        columns (use-columns)
        columns (map
                 (fn [{:keys [header] :as column}]
                   (if (contains? column :header) column
                       (assoc column :header ui/plain-header)))
                 columns)]
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
             (let [w (or
                      (get column :width)
                      (get style :width 100))]
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
  "Abstract component that doesn't need table data"
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
  "Component that is used to distribute received
  dispatch and rows props to proper context. Rest of
  children will than be able to use those contexts."
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
