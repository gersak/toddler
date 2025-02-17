(ns toddler.table
  "Namespace contains context definitions, hooks and component
  that will aid you to build tables faster and with prepared logic.
  
  In rough this namespace contains [[Cell]], [[Row]], [[Header]], [[Body]]
  components that use flex parameters to render table data in table
  layout.
  
  Above components are reusable and aren't ment for direct usage. Instead
  they are here to be extended with more specific implementation. Your
  implementation.
  
  For example look at `toddler.ui.table` where default **:table/row**
  **:table/cell** and **:table** UI components are implemented."
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

(def ^{:dynamic true :no-doc true} ^js *column* (create-context))
(def ^{:dynamic true :no-doc true} ^js *entity* (create-context))
(def ^{:dynamic true :no-doc true} ^js *columns* (create-context))
(def ^{:dynamic true :no-doc true} ^js *actions* (create-context))
(def ^{:dynamic true :no-doc true} ^js *row-record* (create-context))
(def ^{:dynamic true :no-doc true} ^js *rows* (create-context))
(def ^{:dynamic true :no-doc true} ^js *dispatch* (create-context))

(defhook use-columns "Hook will return *columns* context" [] (hooks/use-context *columns*))
(defhook use-column "Hook will return *column* context" [] (hooks/use-context *column*))
(defhook use-rows "Hook will return *rows* context" [] (hooks/use-context *rows*))
(defhook use-row [] "Hook will return *row-record* context" (hooks/use-context *row-record*))

(defhook use-dispatch [] "Hook will return dispatch function for table" (hooks/use-context *dispatch*))

(defnc ^:no-doc NotImplemented
  "Component that will not render anything. It will report
  in js console that this type of field isn't implemented"
  []
  (let [field (use-column)]
    (.error js/console (str "Field not implemented\n%s" (pr-str field))))
  nil)

(defnc Cell
  "Component that abstaracts cell. Component will render div that
  is has preconfigured flex layout that matches column definition.
  
  If class or className is passed to Cell it will be added to that div
  component. Actually every prop will be passed through except:
  
  :width, :level, :cell, :column props
  
  Cell will look for render function under :cell key of column
  definition."
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
        components (hooks/use-context ui/__components__)
        render (hooks/use-memo
                 [render]
                 (if (keyword? render)
                   (get components render)
                   render))
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
      & (dissoc props :style :width :level :cell :column)}
     (provider
      {:context *column*
       :value column}
      ($ render)))))

(defhook use-table-width
  "Hook will return table width by summing all columns widths."
  []
  (let [columns (use-columns)]
    (reduce + 0 (map #(get-in % [:style :width] (get % :width 100)) columns))))

(defnc FRow
  "Flex implementation for table row. It will render
  div and all passed children. 
  
  div will be styled with flex parameters and computed table
  width so that row has min width and when possible to scale
  to fit larger container."
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
  "Reusable component that will propagate column definition and provide
  *row-record* context. This component will render FRow component and in
  it it will render columns by rendering UI :table/cell component and passing
  :column definition in props.
  
  Usually this component is some derivate of [[Cell]] component.
  
  Row can contain children, so this is nice place to put additional content
  like when expanding row and such.
  
  This component, same as Cell can be customized by adding class, className or
  in props, so you can style it to your desires, but logic is reausable."
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
  (let [columns (use-columns)
        cell (ui/use-component :table/cell)]
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
              ($ cell {:key _key :column column})))
          (remove :hidden columns)))
      (c/children props)))))

(defhook use-cell-state
  "Hook that will return vector of

  `[value set-value! select-value!]`
  
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
(defnc ^:no-doc ColumnNameElement
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
  "Function that will apply default styles to column if style
  params aren't provided. Following props are important for column
  to be well styled:
  
   * width - prop that will be read from :width key of column definition
  or [:style :width] position of column definition
   * align - :align key of column definition, can be keyword or set of
  keywords, combination of :left, :top, :right, :bottom, :center
  
  Alignement will adjust cell flex params for justify-content and align-items"
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
  "Hook that will update :columns in recevied props. It will
  map [[column-default-style]] to columns and add :idx prop as well"
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
  "Reusable component that will render table header. It will use
  :header component from column definition if it is available or
  ui :header/plain component from UI context to render header row.
  
  If column has :name specified and header key isn't present it will
  also use :header/plain to render that column. If you wan't to
  ommit header, that you have to **explicitly set :header nil** in
  column definition.
  
  Header row is wrapped in simplebar, so that it can be scrolled
  horizontally. Scroll is hidden and it is assumed that parent component
  will controll and synchronize scroll offset for both body and
  header component."
  {:wrap [(ui/forward-ref)]}
  [{:keys [className class]} _ref]
  (let [{container-width :width} (layout/use-container-dimensions)
        table-width (use-table-width)
        rows (use-rows)
        style {:minWidth table-width}
        columns (use-columns)
        columns (map
                 (fn [column]
                   (if (contains? column :header) column
                       (assoc column :header ui/plain-header)))
                 columns)
        components (hooks/use-context ui/__components__)
        simplebar (ui/use-component :simplebar)]
    (when (some :header columns)
      ($ simplebar
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
             (let [render (if (keyword? render)
                            (get components render)
                            render)
                   w (or
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
  "Reusable component that will render table rows by using component
  under :table/row key in UI Context. It will wrap list of :table/row
  components in div that will fit available container space.
  
  Simplebar is wrapped around that div, so scrolling vertically and
  horizontally over table body comes out of the box.
  
  You can style or add classes using :class or :className props that
  will be propagated to simplebar-content-wrapper div."
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
                              (sequential? class) (into class)))
        table-row (ui/use-component :table/row)
        simplebar (ui/use-component :simplebar)]
    (when (and container-width container-height)
      ($ simplebar
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
             ($ table-row
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
