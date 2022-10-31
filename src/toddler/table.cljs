(ns toddler.table
  (:require
    [cljs-bean.core :refer [->clj]]
    [clojure.string :as str]
    goog.string
    [vura.core :refer [round-number]]
    [helix.dom :as d]
    [helix.core 
     :refer [defhook defnc memo
             create-context $
             <> provider]]
    [helix.hooks :as hooks]
    [helix.children :as c]
    [helix.spring :as spring]
    [helix.styled-components :refer [defstyled]]
    [toddler.ui :as ui]
    [toddler.ui.provider :refer [ExtendUI UI]]
    [toddler.layout :as layout]
    [toddler.dropdown :as dropdown]
    [toddler.ui.default.fields :as fields]
    ; [toddler.elements :as toddler]
    [toddler.hooks
     :refer [use-delayed
             use-dimensions
             use-translate]]
    [toddler.input
     :refer [TextAreaElement]]
    [toddler.popup :as popup]
    [toddler.ui.default.elements :as e]
    ["react" :as react]
    ["toddler-icons$default" :as icon]))



(def ^:dynamic ^js *column* (create-context))
(def ^:dynamic ^js *entity* (create-context))
(def ^:dynamic ^js *columns* (create-context))
(def ^:dynamic ^js *actions* (create-context))
(def ^:dynamic ^js *row-record* (create-context))
(def ^:dynamic ^js *rows* (create-context))
(def ^:dynamic ^js *dispatch* (create-context))
(def ^:dynamic ^js *cell-renderer* (create-context))
(def ^:dynamic ^js *header-renderer* (create-context))
(def ^:dynamic ^js *pagination* (create-context))



(defhook use-pagination [] (hooks/use-context *pagination*))
(defhook use-actions [] (hooks/use-context *actions*))
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
  [{{:keys [style level]
     render :cell
     :as column} :column
    :keys [className]
    :or {render "div"}}]
  {:wrap [(memo #(= (:column %1) (:column %2)))]}
  (when (nil? render)
    (.error "Cell renderer not specified for culumn " (pr-str column)))
  (let [w (get style :width 100)]
    ;; Field dispatcher
    (d/div
      {:class className
       :style (merge
                (->clj style)
                {:display "flex"
                 :flex (str w  \space 0 \space "auto")
                 :min-width w
                 :width w}) 
       :level level}
      (provider
        {:context *column*
         :value column}
        ($ render)))))


(defhook use-table-width
  []
  (let [columns (use-columns)]
    (reduce + 0 (map #(get-in % [:style :width] 100) columns))))


(defnc FRow
  [{:keys [className] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [min-width (use-table-width)] 
    (d/div
      {:className className
       :style {:display "flex"
               :flexDirection "row"
               :justifyContent "flex-start"
               ; :justifyContent "space-around"
               :flex (str 1 \space 0 \space "auto")
               :minWidth min-width}
       :level (:level props)
       & (cond-> 
           (dissoc props :style :level :class :className :data)
           _ref (assoc :ref _ref))}
      (c/children props))))


(defnc Row
  [{data :data
    :keys [className idx render]
    :as props
    :or {render FRow}}]
  {:wrap [(memo 
            (fn [{idx1 :idx data1 :data} {idx2 :idx data2 :data}]
              (and 
                (= idx1 idx2)
                (= data1 data2))))]}
  (let [columns (use-columns)]
    (provider
      {:context *row-record*
       :value (assoc data :idx idx)}
      (<>
        ($ render
           {:key idx
            :className (str className (if (even? idx) " even" " odd"))
            & (dissoc props :className)}
           (map
             (fn [{:keys [attribute cursor] :as column}]
               (let [_key (or
                            (if (keyword? cursor) cursor
                              (when (not-empty cursor)
                                (str/join "->" (map name cursor))))
                            attribute)]
                 ($ Cell
                    {:key _key
                     :column column})))
             (remove :hidden columns)))
        (c/children props)))))



(defnc Header
  [{{:keys [style level]
     n :label
     render :header
     :as column} :column
    :keys [className]
    :as props}
   _ref]
  {:wrap [(react/forwardRef)]}
  (when (nil? render)
    (.error "Header renderer not specified for culumn " (pr-str column)))
  (let [w (get style :width 100)]
    (d/div
      {:class className
       :style (merge
                (->clj style)
                {:display "flex"
                 :flex (str w  \space 0 \space "auto")
                 :min-width w
                 :width w})
       :level level
       & (cond-> (select-keys props [:onClick :onMouseDown])
           _ref (assoc :ref _ref))}
      (when n
        ($ render
           {:column (assoc column :style nil)
            :onChange identity})))))


(defn same-header-row [a b]
  (=
   (select-keys a [:className])
   (select-keys b [:className])))


(defnc HeaderRow
  [{:keys [className]}
   _ref]
  {:wrap [(react/forwardRef)
          (memo same-header-row)]}
  (let [columns (use-columns)]
    ($ ui/row
       {:className className
        & (when _ref {:ref _ref})}
       (map 
         (fn [{a :attribute
               p :cursor
               :as column}]
           ($ ui/header
             {:key (if (nil? p) a
                     (if (keyword? p) p
                       (str/join "->" (map name (not-empty p)))))
              :className "th"
              :column column}))
         (remove :hidden columns)))))


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


(defnc Pagination
  [{:keys [className]}]
  (let [{:keys [page page-size
                next? previous? options
                page-count total-count]
         :as pagination} (use-pagination)
        dispatch (use-dispatch)
        set-pagination! (hooks/use-memo
                          [dispatch]
                          (fn [v] 
                            (dispatch 
                              {:type :pagination/update
                               :pagination v})))
        rows (use-rows)]
    (d/div
      {:class className}
      (when (and pagination (pos? rows) (> total-count (count rows)))
        (d/button
          {:onClick #(set-pagination! {:page 0})
           :className "start"
           :disabled (not previous?)}
          ($ icon/paginationFarPrevious))
        (d/button
          {:onClick #(set-pagination! {:page (dec page)})
           :className "previous"
           :disabled (not previous?)}
          ($ icon/paginationPrevious))
        (d/button
          {:onClick #(set-pagination! {:page (inc page)})
           :className "next"
           :disabled (not next?)}
          ($ icon/paginationNext))
        (d/button
          {:onClick #(set-pagination! {:page (dec page-count)})
           :className "end"
           :disabled (not next?)}
          ($ icon/paginationFarNext))
        (d/span 
          (goog.string/format
            "Showing %d - %d of %d results" 
            (* page page-size)
            (+ (* page page-size) (count rows))
            total-count))
        (d/select
          {:value page-size
           :className "view-size"
           :onChange (fn [e] (set-pagination! {:page-size (js/Number (.. e -target -value))}))}
          (map
            (fn [option]
              (d/option
                {:key option
                 :value option}
                (str "Show " option)))
            (or options (range 10 60 10))))
        (d/span (str "| Page: "))
        ($ ui/idle-input
           {:placeholder "?"
            :className "pag"
            :spell-check false
            :auto-complete "off"
            :type "number"
            :value (inc page)
            :onChange (fn [page] 
                        (let [page (if page page 0)
                              np (min (max 0 (dec page)) page-count)]
                          (set-pagination! {:page np})))})))))


(defnc AddRowAction
  [{:keys [tooltip render name]
    :or {render ui/action}}]
  (let [dispatch (use-dispatch)]
    ($ render 
       {:onClick #(dispatch {:topic :table.row/add})
        :tooltip tooltip
        :icon icon/add}
       name)))


(defnc Actions
  [{:keys [className]}]
  (let [actions (use-actions)]
    (d/div
      {:className className}
      (map
        (fn [{:keys [id render]}]
          ($ render 
            {:key id 
             :className "action" 
             & (dissoc ui/action :render)}))
        actions))))


; (defnc Interactions
;   [{:keys [className]
;     ractions :render/actions
;     rpagination :render/pagination
;     :or {rpagination Pagination
;          ractions Actions}}
;    _ref]
;   {:wrap [(react/forwardRef)]}
;   (let [{:keys [total-count] :as pagination} (use-pagination)
;         actions (use-actions)
;         rows (use-rows)
;         showing-pagination? (and pagination (> total-count (count rows)))]
;     (when (or pagination actions)
;       (d/div
;         {:className className
;          :style {:display "flex"
;                  :justify-content
;                  (cond
;                    (and showing-pagination? (not-empty actions)) "space-between"
;                    showing-pagination? "flex-start"
;                    actions "flex-end")}
;          :ref _ref}
;         (<>
;           (when pagination ($ rpagination {:className "pagination"}))
;           (when (not-empty actions) ($ ractions {:className "actions"})))))))


(defnc TableLayout
  [{:keys [className]}]
  (let [{container-width :width
         container-height :height
         :as container-dimensions} (layout/use-container-dimensions)
        rows (use-rows)
        ; [{container-width :width
        ;   container-height :height} set-container!] (hooks/use-state nil) 
        table-width (use-table-width)
        tbody (hooks/use-ref nil)
        body-scroll (hooks/use-ref nil)
        header-scroll (hooks/use-ref nil)
        [thead {header-height :height}] (use-dimensions)
        header-height (round-number header-height 1 :up)
        table-height (round-number (- container-height header-height) 1 :down)
        scroll (hooks/use-ref nil)
        style {:minWidth table-width}]
    (when (nil? container-dimensions)
      (.error js/console "Wrap Table in container"))
    (hooks/use-effect
      [@body-scroll @header-scroll]
      (letfn [(sync-body-scroll [e]
                (when-some [left (.. e -target -scrollLeft)]
                  (when (and @header-scroll (not= @scroll left)) 
                    (reset! scroll left)
                    (aset @header-scroll "scrollLeft" left))))
              (sync-header-scroll [e]
                (when-some [left (.. e -target -scrollLeft)]
                  (when (and @body-scroll (not= @scroll left)) 
                    (reset! scroll left)
                    (aset @body-scroll "scrollLeft" left))))]
        (when @body-scroll
          (when @header-scroll
            (.addEventListener @header-scroll "scroll" sync-header-scroll))
          (when @body-scroll
            (.addEventListener @body-scroll "scroll" sync-body-scroll))
          (fn [] 
            (when @body-scroll
              (.removeEventListener @body-scroll "scroll" sync-body-scroll))
            (when @header-scroll
              (.removeEventListener @header-scroll "scroll" sync-header-scroll))))))
    (when (and container-width container-height)
      (d/div
        {:key :table
         :className className
         :style {:height container-height
                 :width container-width}}
        #_($ ui/simplebar
           {:key :thead/simplebar
            :scrollableNodeProps #js {:ref #(reset! header-scroll %)}
            :className "thead"
            :$hidden (boolean (not-empty rows))
            :style {:minWidth container-width
                    :maxHeight 100}}
           (spring/div
             {:key :thead
              :ref #(reset! thead %) 
              :style style}
             ($ ui/table-header-row
                {:key :thead/row
                 :className "trow"})))
        ($ ui/simplebar
           {:key :tbody/simplebar
            ; :scrollableNodeProps #js {:ref #(reset! body-scroll %)}
            :className (str "tbody" (when (empty? rows) " empty"))
            ; :style {:minWidth container-width
            ;         :maxHeight table-height}
            }
           (spring/div
             {:key :tbody
              :style style 
              :ref #(reset! tbody %)}
             (map-indexed
               (fn [idx row]
                 ($ ui/table-row
                   {:key (or 
                           (:euuid row)
                           idx)
                    :idx idx
                    :className "trow" 
                    :data row}))
               rows)))))))


(def $action-input
  {(str \&
        ui/wrapper
        #_toddler/dropdown-field-wrapper 
        \space 
        ui/discard
        #_toddler/dropdown-field-discard)
   {:color "transparent"}
   ;;
   (str \& ui/wrapper #_toddler/dropdown-field-wrapper ":hover")
   {(str ui/discard #_toddler/dropdown-field-discard) {:color "inherit"}}})


(defnc ClearButton
  [{:keys [className]}]
  (let [dispatch (use-dispatch)
        column (use-column)
        row (use-row)]
    (when row 
      (d/span
        {:className className
         :onClick #(dispatch
                     {:topic :table.cell/clear
                      :row row
                      :column column})}
        ($ icon/clear)))))


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
                     [idx]
                     (fn []
                       (dispatch
                         {:topic :table.element/change
                          :idx idx 
                          :column el
                          :value value})))]
    [value set-value!]))



(defnc UUIDCell
  [{:keys [className] :as props}]
  (let [[visible? set-visible!] (hooks/use-state nil)
        hidden? (use-delayed (not visible?) 300)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)
        [copied? set-copied!] (hooks/use-state false)
        column (use-column)
        [value] (use-cell-state column)]
    ($ popup/Area
       {:ref area
        :onMouseLeave (fn [] (set-visible! false))
        :onMouseEnter (fn [] 
                        (set-copied! nil)
                        (set-visible! true))}
       ($ ui/button
          {:className className
           :context :fun
           :onClick (fn [] 
                      (when-not copied?
                        (.writeText js/navigator.clipboard (str value))
                        (set-copied! true)))}
          ($ icon/uuid))
       (when visible?
         ($ popup/Element
            {:ref popup
             :$copied copied?
             :style {:visibility (if hidden? "hidden" "visible")
                     :animationDuration ".5s"
                     :animationDelay ".3s"}
             :preference popup/cross-preference
             :className (str  "uuid-popup" (when copied? " copied"))}
            (d/div {:class "info-tooltip"} (str value)))))))


(defnc HashedCell
  [{:keys [className]}]
  (let [{:keys [placeholder] :as element} (use-column)
        [value set-value!] (use-cell-state element)] 
    (d/input
      {:className className
       :placeholder placeholder
       :spell-check false
       :auto-complete "off"
       :type "password"
       :value (or value "") 
       :onChange (fn [e] (set-value! (not-empty (.. e -target -value))))})))


;; TODO - this should be better
(defnc EnumCell
  [{:keys [className]}]
  (let [{:keys [placeholder options] :as column} (use-column)
        [value set-value!] (use-cell-state column)
        [options' om vm] 
        (hooks/use-memo
          [options]
          (let [options' (range (count options))]
            [options'
             (reduce
               (fn [r idx]
                 (assoc r idx (get options idx)))
               nil
               options')
             (reduce
               (fn [r idx]
                 (assoc r (get-in options [idx :value]) idx))
               nil
               options')]))]
    ($ ui/dropdown
       {:value (get vm value) 
        :className className
        :searchable? false
        :search-fn #(get-in om [% :name])
        :onChange #(set-value! (get-in om [% :value]))
        :options options' 
        :placeholder placeholder})))


(defnc TextCell
  [{:keys [className]}]
  (let [{:keys [placeholder options read-only] 
         {:keys [width]} :style
         :as column} (use-column)
        [value set-value!] (use-cell-state column)] 
    ($ TextAreaElement
       {:value value 
        :className className
        :read-only read-only
        :spellCheck false
        :auto-complete "off"
        :style {:maxWidth width}
        :onChange (fn [e] 
                    (set-value! 
                      (not-empty (.. e -target -value))))
        :options options 
        :placeholder placeholder})))


(defnc TimestampCell
  [{:keys [className]}]
  (let [{:keys [placeholder format read-only disabled] 
         :or {format :datetime}
         :as column} (use-column)
        [value set-value!] (use-cell-state column)] 
    ($ ui/dropdown #_toddler/TimestampDropdownElement
       {:value value
        :onChange (fn [v] (when-not read-only (set-value! v)))
        :format format
        :read-only read-only
        :placeholder placeholder 
        :className className
        :disabled disabled})))


(defn interactive-cell? [{:keys [disabled read-only]}]
  (every? not [disabled read-only]))


(defnc IntegerCell
  [{:keys [className]}]
  (let [{:keys [placeholder read-only disabled] :as column} (use-column)
        [value set-value!] (use-cell-state column)
        translate (use-translate)
        [focused? set-focused!] (hooks/use-state false)]
    ($ ui/autosize-input
       {:className className
        :value (if value
                 (if focused? 
                   (str value) 
                   (translate value))
                 "")
        :placeholder placeholder
        :read-only read-only
        :disabled disabled
        :onFocus #(set-focused! true)
        :onBlur #(set-focused! false)
        :onChange (fn [e] 
                    (let [number (.. e -target -value)]
                      (when-some [value (try
                                          (js/parseInt number)
                                          (catch js/Error _ nil))]
                        (set-value! value))))})))


(defnc FloatCell
  [{:keys [className]}]
  (let [{:keys [placeholder read-only disabled] :as column} (use-column)
        [value set-value!] (use-cell-state column)
        translate (use-translate)
        [focused? set-focused!] (hooks/use-state false)]
    ($ ui/autosize-input
      {:className className
       :value (if value
                (if focused? 
                  (str value) 
                  (translate value))
                "")
       :placeholder placeholder
       :read-only read-only
       :disabled disabled
       :onFocus #(set-focused! true)
       :onBlur #(set-focused! false)
       :onChange (fn [e] 
                   (let [number (.. e -target -value)]
                     (when-some [value (try
                                         (js/parseFloat number)
                                         (catch js/Error _ nil))]
                       (set-value! value))))})))


(defnc CurrencyCell
  [{:keys [className]}]
  (let [{:keys [placeholder] :as column} (use-column)
        [value set-value!] (use-cell-state column)] 
    #_($ toddler/CurrencyElement
       {:className className
        :placeholder placeholder
        :value value
        :currency/options nil
        :onChange set-value!})))



(defnc BooleanCell
  [{:keys [className]}]
  (let [{:keys [read-only disabled] :as column} (use-column)
        [value set-value!] (use-cell-state column)] 
    (d/button
      {:disabled disabled
       :read-only read-only
       :className (str className 
                       (case value
                         true " active"
                         (nil false) " inactive"))
       :onClick #(set-value! (not value))}
      ($ (case value
           nil icon/checkboxDefault
           icon/checkbox)))))


(defnc AvatarCell [{:keys [className]}]
  (let [column (use-column)
        [avatar] (use-cell-state column)]
    ($ ui/avatar
       {:avatar avatar
        :size :small
        :className className})))


(defnc IdentityCellInput
  [props]
  ($ UI
    {:components
     {:img fields/field-avatar
      :input "input"
      :wrapper "div"}}
    ($ dropdown/Input
       {& props})))


(defnc IdentityCell [props]
  (let [{:keys [label style placeholder read-only disabled options] :as column} (use-column)
        [value set-value!] (use-cell-state column)]
    ($ dropdown/Element
       {:name label
        :value value
        :onChange (comp set-value! not-empty)
        :search-fn :name
        :placeholder placeholder
        :style (->clj style) 
        :read-only read-only
        :options (when-not read-only options)
        & props}
       (when (every? not [read-only disabled])
         ($ ClearButton
            {:className "clear"})))))


(defstyled identity-cell IdentityCell
  {:display "flex"
   :align-items "center"
   :input {:outline "none"
           :border "none"}})


;; ACTION CELLS
(defnc DeleteCell
  [{:keys [className]}]
  (let [[row] (use-row)
        dispatch (use-dispatch)]
    (d/div
      {:className className 
       :onClick (fn [e] (.stopPropagation e)
                  (dispatch
                    {:topic :table.row/delete
                     :row row}))}
      ($ icon/clear
         {:className "delete-marker"}))))


(defnc ActionCell
  [{:keys [icon]
    :or {icon icon/edit}
    :as props}]
  (d/button
    {& props}
    ($ icon)
    (c/children props)))


(defnc SelectedCell
  [{:keys [className icon]
    :or {icon icon/selectedRow}}]
  (let [column (use-column)
        [value _] (use-cell-state column)]
    (d/div
      {:className className}
      ($ icon
         {:className (str "selected-marker" (when value " selected"))}))))


(defnc ExpandCell
  [{:keys [className]}]
  (let [column (use-column)
        [value set-value!] (use-cell-state column)]
    (d/div
      {:className className 
       :onClick (fn [e] (.stopPropagation e) (set-value! (not value)))}
      ($ icon/expand
         {:className (if value
                       "icon expanded"
                       "icon")}))))


;;;;;;;;;;;;;;;;
;;   HEADERS  ;;
;;;;;;;;;;;;;;;;

(defnc SortElement
  [{{:keys [order]} :column}]
  (case order
    :desc
    ($ icon/sortDesc
       {:className "sort-marker"
        :pull "left"})
    :asc
    ($ icon/sortAsc
       {:className "sort-marker"
        :pull "left"})
    ;;
    ($ icon/sortDesc 
       {:className "sort-marker"
        :pull "left"
        :style #js {:opacity "0"}})))


(defnc ColumnNameElement
  [{{column-name :label
     :keys [order]
     :as column} 
    :column}]
  (let [dispatch (use-dispatch)] 
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
      column-name)))


(defnc IdentityHeader
  [{{:keys [filter] :as column} :column
    :as header}]
  (let [{v :_ilike
         :or {v ""}} filter
        dispatch use-dispatch] 
    (d/div
      {:className (:className header)}
      (d/div
        {:className "header"}
        ($ SortElement {& header})
        ($ ColumnNameElement {& header}))
      (d/div
        {:className "filter"}
        ($ ui/idle-input
           {:placeholder "Filter..."
            :className "filter"
            :spellCheck false
            :auto-complete "off"
            :value (or v "")
            :onChange (fn [value]
                        (when dispatch
                          (dispatch
                            {:type :table.column/filter
                             :column column
                             :value (if (empty? value) nil 
                                      {:name
                                       {:_ilike (str \% (str/replace value #"\s+" "%") \%)}})})))})))))


(defnc TextHeader
  [{{:keys [filter] :as column} :column
    :as header}]
  (let [{v :_ilike
         :or {v ""}} filter
        dispatch (use-dispatch)] 
    (d/div
      {:className (:className header)}
      (d/div
        {:className "header"}
        ($ SortElement {& header})
        ($ ColumnNameElement {& header}))
      (d/div
        {:className "filter"}
        ($ ui/idle-input
           {:placeholder "Filter..."
            :className "filter"
            :spellCheck false
            :auto-complete "off"
            :value (or v "")
            :onChange (fn [value]
                        (when dispatch
                          (dispatch
                            {:type :table.column/filter
                             :column column
                             :value (if (empty? value) nil 
                                      {:_ilike value})})))})))))


(defnc TimestampHeader
  [{{:keys [filter]
     :as column} :column
    :as header}]
  (let [{from  :_ge
         to :_le} filter
        dispatch (use-dispatch)]
    (d/div
      {:className (:className header)}
      (d/div
        {:className "header"}
        ($ SortElement {& header})
        ($ ColumnNameElement {& header}))
      ($ ui/dropdown #_toddler/PeriodDropdownElement
         {:value [from to]
          :placeholder "Filter period..."
          :className "filter"
          ;FIXME
          :onChange (fn [[from to]] 
                      (dispatch
                        {:type :table.column/filter
                         :column column
                         :value (when (or to from) [from to])}))}))))


(def popup-menu-preference
  [#{:bottom :center} 
   #{:left :center} 
   #{:right :center} 
   #{:top :center}])


(defnc BooleanFilter
  [{:keys [onChange className]}]
  (d/div
    {:className className}
    ($ icon/checkbox
       {:className "active" 
        :onClick #(onChange true)})
    ($ icon/checkbox
       {:className "inactive"
        :onClick #(onChange false)})
    ($ icon/checkboxDefault
       {:className "inactive" 
        :onClick #(onChange nil)})))


(defnc BooleanHeader 
  [{{:keys [filter] :as column} :column
    :as header}]
  (let [{v :_eq} filter
        [opened? set-opened!] (hooks/use-state nil)
        dispatch (use-dispatch) 
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (popup/use-outside-action
      opened? area popup
      #(set-opened! false))
    (letfn [(toggle [v]
              (set-opened! false)
              (dispatch
                {:type :table.column/filter
                 :column column
                 :value (when (some? v) {:_eq v})}))] 
      (d/div
        {:className (:className header)}
        (d/div 
          {:className "header"}
          ($ SortElement {& header})
          ($ ColumnNameElement {& header}))
        ($ popup/Area
           {:ref area}
           (d/div
             {:className "filter"
              :onClick (fn [] (set-opened! true))}
             ($ (if (some? v) icon/checkbox icon/checkboxDefault)
                {:className (if v "active" "inactive")}))
           (when opened?
             ($ popup/Element
                {:ref popup
                 :preference popup-menu-preference
                 ; FIXME
                 ; :wrapper toddler/dropdown-popup
                 }
                ($ ui/popup {:onChange toggle}))))))))


(defnc EnumHeader
  [{{:keys [filter]
     :as column} :column
    :as header}]
  (let [{v :_in} filter
        options (map 
                  (comp keyword :name) 
                  (get-in column [:configuration :values]))
        [opened? set-opened!] (hooks/use-state nil)
        dispatch (use-dispatch) 
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (popup/use-outside-action
      opened? area popup
      #(set-opened! false))
    (d/div
      {:className (:className header)}
      (d/div 
        {:className "header"}
        ($ SortElement {& header})
        ($ ColumnNameElement {& header}))
      ($ popup/Area
         {:ref area
          :onClick (fn [e]
                     (when opened? 
                       (.preventDefault e)))}
         (d/div
           {:className "filter"
            :onClick #(set-opened! true)}
           ($ ui/checkbox
              {:active (if (nil? v) nil (boolean (not-empty v)))}))
         (when (and (not-empty options) opened?) 
           ($ popup/Element
              {:ref popup
               :wrapper ui/wrapper
               :preference popup-menu-preference}
              ($ ui/checklist
                 {:value v
                  :multiselect? true
                  :options options 
                  :display-fn name
                  :onChange #(dispatch
                               {:type :table.column/filter
                                :column column
                                :value (when (not-empty %) {:_in (map keyword %)})})})))))))


(defnc PlainHeader
  [header]
  (d/div
    {:className (:className header)}
    (d/div 
      {:className "header"}
      ($ SortElement {& header})
      ($ ColumnNameElement {& header}))))


;; Styled headers
(def header-style
  {:display "flex"
   :flex-direction "column"
   :font-size "1em"
   :height "100%"
   :justify-content "space-between"
   ".header" 
   {:display "flex"
    :flex-direction "row"
    ".name" {:cursor "pointer" :font-weight "600"}}
   ".filter"
   {:margin "4px 0"}})


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
  [{:keys [columns ] :as props}]
  (hooks/use-memo
    [columns]
    (-> props
        (update :columns
                (fn [columns]
                  (mapv column-default-style columns))))))


(defnc Table
  [{:keys [dispatch rows actions pagination]
    :as props}]
  (let [{:keys [columns]} (use-table-defaults props)]
    (provider
      {:context *actions*
       :value actions}
      (provider
        {:context *pagination*
         :value pagination}
        (provider
          {:context *dispatch*
           :value dispatch}
          (provider
            {:context *columns*
             :value columns}
            (provider
              {:context *rows*
               :value rows}
              ($ TableLayout
                 {& props}))))))))
