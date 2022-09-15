(ns toddler.elements.table
  (:require
    [vura.core :refer [round-number]]
    [cljs-bean.core :refer [->clj]]
    [clojure.string :as str]
    goog.string
    [helix.dom :as d]
    [helix.core 
     :refer [defhook defnc memo
             create-context $
             <> provider]]
    [helix.hooks :as hooks]
    [helix.children :as c]
    [helix.placenta.util
     :refer [deep-merge]]
    [helix.styled-components
     :refer [defstyled
             --themed]]
    [helix.spring :as spring]
    [toddler.elements :as toddler]
    [toddler.hooks
     :refer [use-delayed
             use-dimensions
             use-translate]]
    [toddler.elements.input
     :refer [TextAreaElement]]
    [toddler.elements.popup :as popup]
    [toddler.elements.tooltip :as tip]
    ["react" :as react]
    ["react-icons/fa"
     :refer [FaPlus
             FaMinus
             FaCheck
             FaBarcode
             FaTimes
             FaEdit
             FaCaretUp
             FaCaretDown
             FaCaretRight
             FaAngleDoubleLeft
             FaAngleDoubleRight
             FaAngleLeft
             FaAngleRight]]))



(def ^:dynamic *column* (create-context))
(def ^:dynamic *entity* (create-context))
(def ^:dynamic *columns* (create-context))
(def ^:dynamic *actions* (create-context))
(def ^:dynamic *row-record* (create-context))
(def ^:dynamic *rows* (create-context))
(def ^:dynamic *dispatch* (create-context))
(def ^:dynamic *cell-renderer* (create-context))
(def ^:dynamic *header-renderer* (create-context))
(def ^:dynamic *pagination* (create-context))



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


(defhook use-cell-renderer [column]
  (let [f (hooks/use-context *cell-renderer*)]
    (if (ifn? f) (or (f column) NotImplemented)
      NotImplemented)))


(defnc Cell
  [{{:keys [style level]
     :as column} :column
    :keys [className]}]
  {:wrap [(memo #(= (:column %1) (:column %2)))]}
  (let [render (use-cell-renderer column)
        w (get style :width 100)]
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
    (reduce + 0 (map (comp :width :style) columns))))


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


(defhook use-header-renderer [column]
  (let [f (hooks/use-context *header-renderer*)]
    (if (ifn? f) (or (f column) NotImplemented)
      NotImplemented)))


(defnc Header
  [{{:keys [style level]
     n :label
     :as column} :column
    :keys [className]
    :as props}
   _ref]
  {:wrap [(react/forwardRef)]}
  (let [render (use-header-renderer column)
        w (get style :width 100)]
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
   (select-keys a [:className :render/row :render/header])
   (select-keys b [:className :render/row :render/header])))


(defnc HeaderRow
  [{:keys [className]
    rrender :render/row
    rheader :render/header
    :or {rheader Header 
         rrender FRow}}
   _ref]
  {:wrap [(react/forwardRef)
          (memo same-header-row)]}
  (let [columns (use-columns)]
    ($ rrender
       {:className className
        & (when _ref {:ref _ref})}
       (map 
         (fn [{a :attribute
               p :cursor
               :as column}]
           ($ rheader
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
          ($ FaAngleDoubleLeft))
        (d/button
          {:onClick #(set-pagination! {:page (dec page)})
           :className "previous"
           :disabled (not previous?)}
          ($ FaAngleLeft))
        (d/button
          {:onClick #(set-pagination! {:page (inc page)})
           :className "next"
           :disabled (not next?)}
          ($ FaAngleRight))
        (d/button
          {:onClick #(set-pagination! {:page (dec page-count)})
           :className "end"
           :disabled (not next?)}
          ($ FaAngleDoubleRight))
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
        ($ toddler/idle-input
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
    :or {render toddler/action}}]
  (let [dispatch (use-dispatch)]
    ($ render 
       {:onClick #(dispatch {:topic :table.row/add})
        :tooltip tooltip
        :icon FaPlus}
       name)))


(defnc Actions
  [{:keys [className]}]
  (let [actions (use-actions)]
    (d/div
      {:className className}
      (map
        (fn [{:keys [id render] 
              :or {render toddler/Action}
              :as action}]
          ($ render 
            {:key id 
             :className "action" 
             & (dissoc action :render)}))
        actions))))


(defnc Interactions
  [{:keys [className]
    ractions :render/actions
    rpagination :render/pagination
    :or {rpagination Pagination
         ractions Actions}}
   _ref]
  {:wrap [(react/forwardRef)]}
  (let [{:keys [total-count] :as pagination} (use-pagination)
        actions (use-actions)
        rows (use-rows)
        showing-pagination? (and pagination (> total-count (count rows)))]
    (when (or pagination actions)
      (d/div
        {:className className
         :style {:display "flex"
                 :justify-content
                 (cond
                   (and showing-pagination? (not-empty actions)) "space-between"
                   showing-pagination? "flex-start"
                   actions "flex-end")}
         :ref _ref}
        (<>
          (when pagination ($ rpagination {:className "pagination"}))
          (when (not-empty actions) ($ ractions {:className "actions"})))))))


(defnc TableLayout
  [{rrow :render/row
    rheader :render/header
    :or {rrow Row
         rheader HeaderRow}}]
  (let [rows (use-rows)
        {container-width :width
         container-height :height} (toddler/use-container-dimensions) 
        table-width (use-table-width)
        container (toddler/use-container) 
        tbody (hooks/use-ref nil)
        body-scroll (hooks/use-ref nil)
        header-scroll (hooks/use-ref nil)
        [thead {header-height :height}] (use-dimensions)
        ;;
        table-height (round-number (- container-height header-height) 1 :down)
        ;;
        scroll (hooks/use-ref nil)
        overflowing-horizontal? (neg? (- container-width table-width))
        not-overflowing-horizontal? (not overflowing-horizontal?)
        style {:minWidth table-width}]
    (when (nil? container)
      (.error js/console "Wrap toddler/Table in container"))
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
    (println "CON: " [container-width container-height header-height])
    (when (and container-height container-width)
      (if not-overflowing-horizontal?
        (<>
          (d/div
            {:className "thead"}
            (spring/div
              {:ref #(reset! thead %) 
               :style style}
              ($ rheader 
                 {:className "trow"})))
          ($ toddler/simplebar
             {:scrollableNodeProps #js {:ref #(reset! body-scroll %)}
              :className (str "tbody" (when (empty? rows) " empty"))
              :style #js {:maxHeight table-height}}
             (spring/div
               {:style style 
                :ref #(reset! tbody %)}
               (map-indexed
                 (fn [idx row]
                   ($ rrow
                     {:key (or (:euuid row) idx)
                      :idx idx
                      :className "trow" 
                      :data row}))
                 rows))))
        (let [final-width container-width]
          (<>
            ($ toddler/simplebar
               {:scrollableNodeProps #js {:ref #(reset! header-scroll %)}
                :className "thead"
                :$hidden (boolean (not-empty rows))
                :style #js {:minWidth final-width
                            :maxHeight 100}}
               (spring/div
                 {:ref #(reset! thead %) 
                  :style style}
                 ($ rheader 
                    {:className "trow"})))
            ($ toddler/simplebar
               {:scrollableNodeProps #js {:ref #(reset! body-scroll %)}
                :className (str "tbody" (when (empty? rows) " empty"))
                :style #js {:minWidth final-width 
                            :maxHeight table-height}}
               (spring/div
                 {:style style 
                  :ref #(reset! tbody %)}
                 (map-indexed
                   (fn [idx row]
                     ($ rrow
                       {:key (or 
                               (:euuid row)
                               idx)
                        :idx idx
                        :className "trow" 
                        :data row}))
                   rows)))))))))


(defstyled table-button toddler/button
  {:display "flex"
   :justify-content "center"
   :align-items "center"
   :font-size "10"
   :height 20
   :min-height 20
   :padding 0
   :margin "0px !important"})


(defstyled uuid-button table-button
  (let [size 20]
    {:min-width size
     :min-height size
     :height size :width size}))


(def $action-input
  {(str \&
        toddler/dropdown-field-wrapper 
        \space 
        toddler/dropdown-field-discard)
   {:color "transparent"}
   ;;
   (str \& toddler/dropdown-field-wrapper ":hover")
   {(str toddler/dropdown-field-discard) {:color "inherit"}}})


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
        ($ FaTimes)))))


(defstyled clear-button ClearButton
  {:position "absolute"
   :right 0
   :top 6
   :transition "color .2s ease-in-out"}
  --themed)


;;;;;;;;;;;
;; CELLS ;;
;;;;;;;;;;;


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
  [{:keys [className]}]
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
       ($ uuid-button
          {:context :fun
           :onClick (fn [] 
                      (when-not copied?
                        (.writeText js/navigator.clipboard (str value))
                        (set-copied! true)))}
          ($ FaBarcode))
       (when visible?
         ($ popup/Element
            {:ref popup
             :$copied copied?
             :style {:visibility (if hidden? "hidden" "visible")
                     :animationDuration ".5s"
                     :animationDelay ".3s"}
             :preference popup/cross-preference
             :className (str className " animated fadeIn" 
                             (when copied? " copied"))}
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
    ($ toddler/DropdownElement
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
    ($ toddler/TimestampDropdownElement
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
    ($ toddler/autosize-input
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
    ($ toddler/autosize-input
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
    ($ toddler/CurrencyElement
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
           nil FaMinus 
           FaCheck)))))


(defstyled uuid-cell UUIDCell
  (assoc tip/basic ".info-tooltip" tip/basic-content)
  --themed)


(defstyled enum-cell EnumCell
  {:input {:font-size 12
           :font-weight "600"
           :cursor "pointer"}}
  --themed)


(defstyled text-cell TextCell
  {:font-size "12"
   :border "none"
   :outline "none"
   :resize "none"
   :padding 0
   :width "100%"}
  --themed)


(defstyled timestamp-cell TimestampCell
  {:display "flex"
   :justify-content "center"
   :input
   {:font-size "12"
    :border "none"
    :outline "none"
    :resize "none"
    :padding 2
    :width "100%"}}
  ;;
  --themed)


(defstyled integer-cell IntegerCell
  {:border "none"
   :outline "none"
   :font-size "12"
   :width "90%"}
  --themed)


(defstyled float-cell FloatCell
  {:border "none"
   :outline "none"
   :font-size "12"}
  --themed)


(defstyled currency-cell CurrencyCell
  {:font-size "12"
   :max-width 140
   :display "flex"
   :align-items "center"
   :input {:outline "none"
           :border "none"
           :max-width 100}}
  --themed)

(defstyled boolean-cell BooleanCell
  {:font-size 12
   :padding 0
   :width 20
   :height 20
   :display "flex"
   :justify-content "center"
   :align-items "center"
   :transition "background-color .3s ease-in-out"}
  --themed)


(defnc UserCellInput
  [props]
  (let [{:keys [read-only disabled] :as column} (use-column)
        [value] (use-cell-state column)]
    ($ toddler/DropdownElement
       {:render/img toddler/UserDropdownAvatar
        :value value
        & props}
       (when (every? not [read-only disabled])
         ($ ClearButton
            {:className "clear"})))))


(defstyled user-cell-input UserCellInput
  (assoc $action-input :display "flex" :align-items "center"
    ".clear" {:transition "color .2s ease-in-out"})
  --themed)


(defnc AvatarCell [{:keys [className]}]
  (let [column (use-column)
        [avatar] (use-cell-state column)]
    ($ toddler/avatar
       {:avatar avatar
        :size :small
        :className className})))


(defnc UserDropdownOption
  [{:keys [option] :as props} ref]
  {:wrap [(react/forwardRef)]}
  ($ toddler/dropdown-option
    {:ref ref
     & (dissoc props :ref)}
    ($ toddler/avatar {:size :small & option})
    (:name option)))


(defnc UserDropdownPopup
  [props]
  ($ toddler/DropdownPopup
    {:render/option UserDropdownOption
     & props}))


(defstyled user-dropdown-popup UserDropdownPopup
  {:max-height 250})


(defnc UserCell [props]
  (let [{:keys [label style placeholder read-only options] :as column} (use-column)
        [value set-value!] (use-cell-state column)]
    ($ UserCellInput
       {:name label
        :value value
        :onChange (comp set-value! not-empty)
        :search-fn :name
        :placeholder placeholder
        :style (->clj style) 
        :read-only read-only
        :options (when-not read-only options)
        :render/input user-cell-input
        :render/popup user-dropdown-popup
        & props})))


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
      ($ FaTimes
         {:className "delete-marker"}))))


(defnc ActionCell
  [{:keys [icon]
    :or {icon FaEdit}
    :as props}]
  (d/button
    {& props}
    ($ icon)
    (c/children props)))


(defnc SelectedCell
  [{:keys [className icon]
    :or {icon FaAngleRight}}]
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
      ($ FaCaretRight
         {:className (if value
                       "icon expanded"
                       "icon")}))))


(defstyled user-cell UserCell 
  {:input {:font-size 12}
   ".clear" {:color "transparent"
             :display "flex"
             :align-items "center"}}
  --themed)

(defstyled action-cell ActionCell
  {:padding 5
   :font-size "10"
   :border-radius 3
   :display "flex"
   :justify-content "center"
   :transition "box-shadow .3s ease-in,background .3s ease-in"
   :border "2px solid transparent"
   :align-items "center"
   :cursor "pointer"
   ":focus" {:outline "none"}}
  --themed)

(defstyled delete-cell DeleteCell
  {:display "flex"
   :justify-content "center"
   :align-content "center"
   :min-height 25
   :min-width 30
   :max-height 30
   :font-size "12"
   :align-items "center"
   ; :margin-top 3
   ".delete-marker"
   {:cursor "pointer"
    :margin "1px 3px"
    :transition "color .3s ease-in"}}
  --themed)

(defstyled expand-cell ExpandCell
  {:display "flex"
   :flex-grow "1"
   :justify-content "center"
   :cursor "pointer"
   :svg {:transition "transform .3s ease-in-out"}}
  --themed)



;;;;;;;;;;;;;;;;
;;   HEADERS  ;;
;;;;;;;;;;;;;;;;

(defnc SortElement
  [{{:keys [order]} :column}]
  (case order
    :desc
    ($ FaCaretUp
       {:className "sort-marker"
        :pull "left"})
    :asc
    ($ FaCaretDown
       {:className "sort-marker"
        :pull "left"})
    ;;
    ($ FaCaretUp
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


(defnc UserHeader
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
        ($ toddler/idle-input
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
        ($ toddler/idle-input
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
      ($ toddler/PeriodDropdownElement
         {:value [from to]
          :placeholder "Filter period..."
          :className "filter"
          :render/field toddler/PeriodInput
          ;; FIXME - PeriodDropdownElement is calling onChange even
          ;; though there were no change. This should be fixed!
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
    ($ FaCheck
       {:className "active" 
        :onClick #(onChange true)})
    ($ FaCheck
       {:className "inactive"
        :onClick #(onChange false)})
    ($ FaMinus
       {:className "inactive" 
        :onClick #(onChange nil)})))


(defnc BooleanHeader 
  [{{:keys [filter] :as column} :column
    rpopup :render/popup
    :as header
    :or {rpopup "div"}}]
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
             ($ (if (some? v) FaCheck FaMinus)
                {:className (if v "active" "inactive")}))
           (when opened?
             ($ popup/Element
                {:ref popup
                 :preference popup-menu-preference
                 :wrapper toddler/dropdown-popup}
                ($ rpopup {:onChange toggle}))))))))


(defnc EnumHeader
  [{{:keys [filter]
     :as column} :column
    rpopup :render/popup
    :or {rpopup "div"}
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
           ($ toddler/checkbox
              {:active (if (nil? v) nil (boolean (not-empty v)))}))
         (when (and (not-empty options) opened?) 
           ($ popup/Element
              {:ref popup
               :wrapper rpopup
               :preference popup-menu-preference}
              ($ toddler/checklist
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
   :font-size "12"
   :height "100%"
   :justify-content "space-between"
   ".header" 
   {:display "flex"
    :flex-direction "row"
    ".name" {:cursor "pointer" :font-weight "600"}}
   ".filter"
   {:margin "4px 0"}})


(defstyled plain-header PlainHeader 
  header-style
  --themed)


(defstyled user-header UserHeader
  (deep-merge
    header-style
    {".filter"
     {:line-height 12
      :padding 0
      ; :flex-grow "1"
      :justify-self "center"
      :resize "none"
      :border "none"
      :width "100%"}})
  --themed)


(defstyled text-header TextHeader 
  (deep-merge
    header-style
    {".filter"
     {:line-height 12
      :padding 0
      ; :flex-grow "1"
      :justify-self "center"
      :resize "none"
      :border "none"
      :width "100%"}})
  --themed)


(defstyled boolean-popup BooleanFilter 
  {:display "flex"
   :flex-direction "row"
 (str toddler/checkbox-button) {:margin "1px 2px"}})


(defstyled boolean-header BooleanHeader 
  (deep-merge 
    header-style
    {:align-items "center"})
  --themed)


(defstyled enum-popup popup/element
  {(str toddler/checklist " .name") {:font-size "12"}})


(defstyled enum-header EnumHeader 
  (deep-merge
    header-style
    {:justify-content "flex-start"
     :align-items "center"
     ; ".header" {:margin-left "-1em"}
     })
  --themed)

(defstyled timestamp-header TimestampHeader
  (deep-merge
    header-style
    {:justify-content "flex-start"
     :align-items "center"
     ; ".header" {:margin-left "-1em"}
     })
  --themed)


(defn header-resolver
  [{:keys [type]}]
  (case  type
    "enum" enum-header
    "boolean" boolean-header
    "string" text-header 
    "user" user-header
    "timestamp" timestamp-header
    plain-header))


(defn cell-resolver
  [{:keys [render type]}]
  (if (some? render) render
    (case type
      :action/delete delete-cell
      :action/expand expand-cell
      "boolean" boolean-cell
      "currency" currency-cell
      "enum" enum-cell
      "float" float-cell
      "hashed" HashedCell
      "int" integer-cell
      "uuid" uuid-cell
      "string" text-cell
      "user" user-cell
      "timestamp" timestamp-cell
      (throw
        (js/Error.
          (str
            "Unknown reneder type: '" type
            "'. Specify either valid :type or :render attribute in column definition"))))))


(defstyled pagination Pagination
  {:padding "2px 3px 8px 3px"
   :display "flex"
   :font-size "12"
   :align-items "center"
   :button {:outline "none"
            :border "none"}
   :input 
   {:margin-left 3
    :border "none"
    :outling "none"
    :width 40}}
  --themed)


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
                  (mapv column-default-style  columns))))))


(defnc Table
  [{:keys [className dispatch rows actions pagination]
    cell-resolver :cell/resolver
    header-resolver :header/resolver
    :or {cell-resolver cell-resolver
         header-resolver header-resolver}
    :as props}]
  (let [{:keys [columns]} (use-table-defaults props)
        [style] (spring/use-spring
                  {:from {:opacity 0}
                   :to {:opacity 1}
                   :delay 200})]
    (spring/div
      {:className className
       :style style}
      (provider
        {:context *actions*
         :value actions}
        (provider
          {:context *pagination*
           :value pagination}
          (provider
            {:context *cell-renderer*
             :value cell-resolver}
            (provider
              {:context *header-renderer*
               :value header-resolver}
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
                       {& props})))))))))))



(defstyled table Table
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"}
  --themed)
