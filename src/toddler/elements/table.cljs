(ns toddler.elements.table
  (:require
    [helix.core :refer [defhook defnc create-context $]]
    [helix.hooks :as hooks]
    [toddler.elements :as toddler]))


(def ^:dynamic *element* (create-context))
(def ^:dynamic *attribute-value* (create-context))
(def ^:dynamic *entity* (create-context))
(def ^:dynamic *relation* (create-context))
(def ^:dynamic *db* (create-context))
(def ^:dynamic *ui* (create-context))
(def ^:dynamic *columns* (create-context))
(def ^:dynamic *actions* (create-context))
(def ^:dynamic *row-record* (create-context))
(def ^:dynamic *rows* (create-context))
(def ^:dynamic *handlers* (create-context))
(def ^:dynamic *datasource* (create-context))
(def ^:dynamic *query* (create-context))
(def ^:dynamic *mutation* (create-context))
(def ^:dynamic *dispatch* (create-context))
(def ^:dynamic *content-container* (create-context))
(def ^:dynamic *content-container-style* (create-context))
(def ^:dynamic *cell-renderer* (create-context))
(def ^:dynamic *header-renderer* (create-context))
(def ^:dynamic *pagination* (create-context))



(defhook use-content-container [] (hooks/use-context *content-container*))
(defhook use-content-container-style [] (hooks/use-context *content-container-style*))
(defhook use-element [] (hooks/use-context *element*))
(defhook use-attribute-value [] (hooks/use-context *attribute-value*))
(defhook use-pagination [] (hooks/use-context *pagination*))


(defhook use-actions [] (hooks/use-context *actions*))
(defhook use-columns [] (hooks/use-context *columns*))
(defhook use-rows [] (hooks/use-context *rows*))
(defhook use-row []
  (let [rr (hooks/use-context *row-record*)
        c (hooks/use-ref rr)]
    (hooks/use-effect 
      [rr]
      (reset! c rr))
    [rr c]))

(defhook use-handlers [] (hooks/use-context *handlers*))
(defhook use-dispatch [] (hooks/use-context *dispatch*))


(defnc AddRowAction
  [{:keys [tooltip render name]
    :or {render toddler/action}}]
  (let [{:keys [add]} (use-handlers)]
    ($ render 
       {:onClick #(add) 
        :tooltip tooltip
        :icon faPlus}
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


(declare RecordProvider)


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
    ($ RecordProvider
       {:row (assoc data :idx idx)}
       (<>
         ($ render
            {:key idx
             :className (str className (if (even? idx) " even" " odd"))
             & (dissoc props :className)}
            (map
              (fn [{:keys [attribute path] :as column}]
                ($ Cell
                  {:key (or
                          (if (keyword? path) path
                            (when (not-empty path)
                              (clojure.string/join "->" (map name path))))
                          attribute)
                   :column column}))
              (filter :visible columns)))
         (c/children props)))))


(defnc NotImplemented
  []
  (let [field (use-element)] 
    (log/errorf "Field not implemented\n%s" (pr-str field)))
  nil)

(defhook use-cell-renderer [column]
  (let [f (hooks/use-context *cell-renderer*)]
    (if (ifn? f) (or (f column) NotImplemented)
      NotImplemented)))

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
    ;; Field dispatcher
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
               p :path
               :as column}]
           ($ rheader
             {:key (if (nil? p) a
                     (if (keyword? p) p
                       (str/join "->" (map name (not-empty p)))))
              :className "th"
              :column column}))
         (filter :visible columns)))))


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
    (log/debugf "Rendering pagination\n%s\nRow count: %d\nTotal count positive? %s\nTotal count > rows? %s" 
                pagination (count rows) (pos? total-count) (> total-count (count rows)))
    (d/div
      {:class className}
      (when (and pagination (pos? rows) (> total-count (count rows)))
        (d/button
          {:onClick #(set-pagination! {:page 0})
           :className "start"
           :disabled (not previous?)}
          ($ toddler/fa {:icon faAngleDoubleLeft}))
        (d/button
          {:onClick #(set-pagination! {:page (dec page)})
           :className "previous"
           :disabled (not previous?)}
          ($ toddler/fa {:icon faAngleLeft}))
        (d/button
          {:onClick #(set-pagination! {:page (inc page)})
           :className "next"
           :disabled (not next?)}
          ($ toddler/fa {:icon faAngleRight}))
        (d/button
          {:onClick #(set-pagination! {:page (dec page-count)})
           :className "end"
           :disabled (not next?)}
          ($ toddler/fa {:icon faAngleDoubleRight}))
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


(defnc Table
  [{rrow :render/row
    rheader :render/header
    rinteractions :render/interactions
    :or {rrow Row
         rheader HeaderRow
         rinteractions Interactions}}]
  (let [rows (use-rows)
        {cmaxh :maxHeight
         ch :height} (use-content-container-style) 
        table-width (use-table-width)
        container (use-content-container) 
        interactions (hooks/use-ref nil)
        thead (hooks/use-ref nil)
        tbody (hooks/use-ref nil)
        body-scroll (hooks/use-ref nil)
        header-scroll (hooks/use-ref nil)
        [header-height set-header-height!] (hooks/use-state 0)
        [interactions-height set-interactions-height!] (hooks/use-state 0)
        ;;
        columns (use-columns)
        ;;
        [{container-width :width}
         set-table-style!] (hooks/use-state nil)
        ;;
        scroll (hooks/use-ref nil)
        overflowing-horizontal? (neg? (- container-width table-width))
        not-overflowing-horizontal? (not overflowing-horizontal?)
        fixed-height? (some? ch)
        no-fixed-height? (not fixed-height?)
        max-height? (some? cmaxh)
        no-max-height? (not max-height?)]
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
    (hooks/use-effect
      [@thead columns]
      (when @thead
        (let [rect (.getBoundingClientRect @thead)
              height (.-height rect)]
          (log/debugf "Columns changed\nHead rect: %s\nHead height: %s" rect height)
          (set-header-height! (round-number (dec height) 1 :down)))))
    (hooks/use-effect
      [@interactions]
      (when @interactions
        (let [rect (.getBoundingClientRect @interactions)
              height (.-height rect)]
          (log/debugf "Interactions changed\nInteractions rect: %s\nInteractions height: %s" rect height)
          (set-interactions-height! (round-number (dec height) 1 :down)))))
    (log/debugf
      "\nContainer width: %s\nTable width: %s\nOverflowing H? %s\nMax height? %s\nFixed height: %s"
      container-width table-width overflowing-horizontal? max-height? fixed-height?)
    ;;
    (hooks/use-effect
      [@container]
      (when @container
        (letfn [(reset [[entry]]
                  (let [content-rect (.-contentRect entry)]
                    (set-table-style!
                      {:width (.-width content-rect)
                       :height (.-height content-rect)})))]
          (let [observer (js/ResizeObserver. reset)]
            (.observe observer @container)
            (fn [] (.disconnect observer))))))
    (cond
      ;;
      (and not-overflowing-horizontal? no-max-height? no-fixed-height?)
      (<>
        ($ rinteractions 
           {:className "interactions"
            :ref #(reset! interactions %)})
        (d/div
          {:className "thead"}
          (d/div
            {:ref #(reset! thead %)}
            ($ rheader {:className "trow"})))
        (d/div
          {:className "tbody"}
          (d/div
            {:ref #(reset! tbody %)}
            (map-indexed
              (fn [idx row]
                ($ rrow
                  {:key (or 
                          (:euuid row)
                          (db/row-primary-key row)
                          idx)
                   :idx idx
                   :className "trow" 
                   :data row}))
              rows))))
      ;;
      (and not-overflowing-horizontal? (or max-height? fixed-height?))
      (<>
        ($ rinteractions 
           {:className "interactions"
            :ref #(reset! interactions %)})
        (d/div
          {:className "thead"}
          (d/div
            {:ref #(reset! thead %) 
             :style {:minWidth table-width}}
            ($ rheader 
               {:className "trow"})))
        ($ toddler/simplebar
           {:scrollableNodeProps #js {:ref #(reset! body-scroll %)}
            :className (str "tbody" (when (empty? rows) " empty"))
            :style #js {:height (when ch (- ch interactions-height header-height 10))
                        :maxHeight (when cmaxh (- cmaxh interactions-height header-height 10))}}
           (d/div
             {:style {:minWidth table-width}
              :ref #(reset! tbody %)}
             (map-indexed
               (fn [idx row]
                 ($ rrow
                   {:key (or 
                           (:euuid row)
                           (db/row-primary-key row)
                           idx)
                    :idx idx
                    :className "trow" 
                    :data row}))
               rows))))
      ;;
      (and overflowing-horizontal? no-max-height? no-fixed-height?)
      (let [final-width container-width]
        (<>
          ($ rinteractions 
             {:className "interactions"
              :ref #(reset! interactions %)})
          ($ toddler/simplebar
             {:scrollableNodeProps #js {:ref #(reset! header-scroll %)}
              :className "thead"
              :$hidden (boolean (not-empty rows))
              :style #js {:minWidth final-width}}
             (d/div
               {:ref #(reset! thead %) 
                :style {:minWidth table-width}}
               ($ rheader 
                  {:className "trow"})))
          ($ toddler/simplebar
             {:scrollableNodeProps #js {:ref #(reset! body-scroll %)}
              :className (str "tbody" (when (empty? rows) " empty"))
              :style #js {:minWidth final-width}}
             (d/div
               {:style {:minWidth table-width}
                :ref #(reset! tbody %)}
               (map-indexed
                 (fn [idx row]
                   ($ rrow
                     {:key (or 
                             (:euuid row)
                             (db/row-primary-key row)
                             idx)
                      :idx idx
                      :className "trow" 
                      :data row}))
                 rows)))))
      ;;
      (and overflowing-horizontal? (or max-height? fixed-height?))
      (let [final-width container-width]
        (<>
          ($ rinteractions 
             {:className "interactions"
              :ref #(reset! interactions %)})
          ($ toddler/simplebar
             {:scrollableNodeProps #js {:ref #(reset! header-scroll %)}
              :className "thead"
              :$hidden (boolean (not-empty rows))
              :style #js {:minWidth final-width
                          :maxHeight 100}}
             (d/div
               {:ref #(reset! thead %) 
                :style {:minWidth table-width}}
               ($ rheader 
                  {:className "trow"})))
          ($ toddler/simplebar
             {:scrollableNodeProps #js {:ref #(reset! body-scroll %)}
              :className (str "tbody" (when (empty? rows) " empty"))
              :style #js {:minWidth final-width 
                          :height (when ch (- ch interactions-height header-height 10))
                          :maxHeight (when cmaxh (- cmaxh interactions-height header-height 10))}}
             (d/div
               {:style {:minWidth table-width}
                :ref #(reset! tbody %)}
               (map-indexed
                 (fn [idx row]
                   ($ rrow
                     {:key (or 
                             (:euuid row)
                             (db/row-primary-key row)
                             idx)
                      :idx idx
                      :className "trow" 
                      :data row}))
                 rows))))))))


(defhook use-table
  [{:keys [db] :as props}]
  (let [[{:keys [columns selection args path auto/refresh pagination]
          :or {refresh true}
          :as state} dispatch]
        (hooks/use-reducer
          reducer props 
          (comp update-table-selection update-table-args))
        [root-row rc] (use-row)
        [entity] path
        ;;
        {:keys [page page-size]} pagination
        ;;
        {:keys [search] :as handlers} 
        (hooks/use-memo
          [entity columns args]
          (letfn [(add
                    ([]
                     (dispatch {:type :table.row/add :row nil}))
                    ([value]
                     (dispatch {:type :table.row/add :row value})))
                  (sync-row
                    ([]
                     (dispatch {:type :table.row/sync :row nil}))
                    ([value]
                     (dispatch {:type :table.row/sync :row value})))
                  (remote-sync-row
                    ([]
                     (dispatch {:type :table.row/sync :row nil :remote? true}))
                    ([value]
                     (dispatch {:type :table.row/sync :row value :remote? true})))
                  (sync
                    ([rows]
                     (dispatch
                       {:type :table/sync
                        :rows rows})))
                  (remote-sync
                    ([rows]
                     (dispatch
                       {:type :table/sync
                        :rows rows
                        :remote? true})))
                  (pull
                    ([] (db/pull db entity selection nil))
                    ([row] (db/pull db entity row selection nil))
                    ([row field] (get (db/pull db entity row selection nil) field)))
                  (delete 
                    ([row] 
                     (dispatch
                       {:type :table.row/delete
                        :row row})))
                  (restore
                    ([row]
                     (log/debugf "Removing row %s" (pr-str row))
                     (dispatch
                       {:type :table.row/restore
                        :row row})))
                  (discard ([] (db/discard! db)))
                  (subordinate-table?
                    [[_ & linked-entity]]
                    (not-empty linked-entity))
                  (count []
                    (go 
                      (let [selection (path->count-selection path)
                            args (dissoc args :_order_by :_limit :_offset)
                            {:keys [data]}
                            (if (subordinate-table? path)
                              (if (not-empty @rc)
                                (do
                                  (log/debugf
                                    "Searching for eywa aggregate\nEntity: %s\nRoot row: %s\nPath: %s\nSelection:\n%s"
                                    entity @rc path (with-out-str (pprint selection)))
                                  (update
                                    (async/<!
                                      (db/eywa-aggregate
                                        db entity 
                                        (cond-> selection
                                          (and (some? (second path)) (not-empty args))
                                          (assoc-in [(second path) 0 :args] 
                                                    (let [args2 (dissoc args :_order_by :_limit :_offset)]
                                                      (when (not-empty args2) {:_where args2}))))
                                        {:euuid {:_eq (:euuid @rc)}}))
                                    :data get-in (rest path)))
                                (do
                                  (log/errorf "Root record not provided!\nEntity: %s\nRow: %s\nPath: %s\nSelection:\n"
                                    entity (assoc-in @rc [(second path) 0 :args] args) path (with-out-str (pprint selection)))
                                  {:data []}))
                              ;;
                              (do
                                (log/debugf "Remote eywa aggregate\nEntity: %s\nArgs: %s\nPath: %s\nSelection:\n"
                                            entity args path (with-out-str (pprint selection)))
                                (async/<! (db/eywa-aggregate db entity selection args))))]
                        (:count data))))
                  (search
                    ([]
                     (go 
                       (let [{:keys [errors data] :as response}
                             (if (subordinate-table? path)
                               (if (not-empty @rc)
                                 (do
                                   (log/debugf "Searching remote EYWA from root row.\nEntity: %s\nRoot row: %s\nPath: %s\nSelection:%s\n"
                                     entity @rc path selection)
                                   (update
                                     (async/<!
                                       (db/eywa-get 
                                         db entity (select-keys @rc [:euuid])
                                         (cond-> (focus-selection-euuids selection)
                                           (and (some? (second path)) (not-empty args))
                                           (assoc-in [(second path) 0 :args] 
                                                     (let [args1 (select-keys args [:_order_by :_limit :_offset])
                                                           args2 (dissoc args :_order_by :_limit :_offset)]
                                                       (cond-> args1
                                                         (not-empty args2) (assoc :_where args2)))))))
                                     :data get-in (rest path)))
                                 (do
                                   (log/warnf
                                     "Searching remote EYWA not possible. Root record not provided \nEntity: %s\nRoot row: %s\nPath: %s\nSelection:%s\n"
                                     entity (assoc-in @rc [(second path) 0 :args] args) path selection)
                                   {:data []}))
                               ;;
                               (do
                                 (log/debugf 
                                   "Searching remote EYWA\nEntity: %s\nArgs: %s\nPath: %s\nSelection:%s\n"
                                   entity args path selection)
                                 (async/<!
                                   (db/eywa-search 
                                     db entity 
                                     (focus-selection-euuids selection) 
                                     args))))]
                         (if-not (empty? errors)
                           (.error js/console "Couldn't execute search query")
                           (dispatch 
                             {:type :db.remote/pulled
                              :rows data}))
                         response))))
                  (mutate
                    ([] (mutate #{:diff :delete :slice}))
                    ([ks]
                     (go
                       (async/<! (db/eywa-sync db ks))
                       (let [{:keys [errors data]}
                             (async/<! (db/eywa-search db entity selection args))]
                         (if (not-empty errors)
                           (.error js/console "Couldn't execute search query")
                           (dispatch 
                             {:type :db.remote/mutated
                              :rows data}))))))]
            {:dispatch dispatch
             :discard discard
             :count count
             :add add
             :remote-sync-row remote-sync-row
             :remote-sync remote-sync
             :sync-row sync-row
             :sync sync
             :pull pull
             :delete delete
             :restore restore
             :search search 
             :mutate mutate}))]
    ;; Track root row and refresh if root row has changed
    (hooks/use-effect
      [root-row]
      (dispatch 
        {:type :root.row/update 
         :root/row root-row}))
    ;; Track column changes and refresh if columns have changed
    (hooks/use-effect
      [root-row columns page page-size]
      (when refresh
        (log/debugf "Refreshing table\nRoot: %s\nColumns: %s\nPagination:"
          (pr-str root-row)
          (pr-str columns)
          (pr-str pagination))
        (when pagination
          (go 
            (let [ocount (async/<! ((:count handlers)))]
              (log/debugf "Updating pagination total count %d" ocount)
              (dispatch
                {:type :pagination/update
                 :pagination {:total-count (or ocount 0)}}))))
        (search)))
    ;; TODO - decide if this is necessary. Subordinate tables will conclude that
    ;; mutations are discared on parent sync, because parent will trigger db/commit!
    ;; Track discard
    (use-delta-monitor
      db 
      (fn [_ old new]
        (when (and (some? old) (nil? new))
          (dispatch {:type :table/commit}))))
    (assoc 
      (select-keys state [:actions :columns :rows :selection
                          :pagination :root/entity :db
                          :table/entity]) 
      :handlers handlers)))
