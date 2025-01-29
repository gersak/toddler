(ns toddler.showcase.table
  (:require
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.grid :as grid]
   [toddler.ui :as ui]
   [toddler.table :as table]
   [toddler.core :as toddler]
   [toddler.provider :as provider]
   [toddler.router :as router]
   [toddler.md.lazy :as md]
   [vura.core :as vura]
   [helix.core :refer [$ defnc]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [helix.children :as c]))

(defn random-user []
  {:euuid (random-uuid)
   :name (rand-nth
          ["John"
           "Emerick"
           "Harry"
           "Ivan"
           "Dugi"
           "Ricky"])})

(def columns
  [{:cursor [:ui :expand]
    :cell ui/expand-cell
    :header nil
    :width 20}
   {:cursor :euuid
    :label "UUID"
    :align :center
    :header nil
    :cell ui/uuid-cell
    :width 50}
   {:cursor :user
    :label "User"
    :cell ui/identity-cell
    :options (repeatedly 3 random-user)
    :width 100}
   {:cursor :float
    :cell ui/float-cell
    :label "Float"
    :width 100}
   {:cursor :integer
    :cell ui/integer-cell
    :label "Integer"
    :width 100}
   {:cursor :text
    :cell ui/text-cell
    :label "Text"
    :width 250}
   {:cursor :currency
    :cell ui/currency-cell
    :width 150
    :label "Money"}
   {:cursor :enum
    :label "ENUM"
    :cell ui/enum-cell
    :options [{:name "Dog"
               :value :dog}
              {:name "Cat"
               :value :cat}
              {:name "Horse"
               :value :horse}
              {:name "Hippopotamus"
               :value :hypo}]
    :placeholder "Choose your fav"
    :width 100}
   {:cursor :timestamp
    :cell ui/timestamp-cell
    :label "Timestamp"
    :show-time false
    :width 120}
   {:cursor :boolean
    :cell ui/boolean-cell
    :label "Boolean"
    :type "boolean"
    :width 50}])

(defn generate-column
  "Function will generate data for input column"
  [{t :cell}]
  (let [now (-> (vura/date) vura/time->value)]
    (letfn [(rand-date
              []
              (->
               now
               (+ (* (rand-nth [1 -1])
                     (vura/hours (rand-int 1000)))
                  (vura/minutes (rand-int 60)))
               vura/value->time))]
      (condp = t
        ui/uuid-cell (random-uuid)
        ui/integer-cell (rand-int 10000)
        ui/float-cell (* (rand) 1000)
        ui/identity-cell (random-user)
        ui/currency-cell {:amount (vura/round-number (* 1000 (rand)) 0.25)
                          :currency (rand-nth ["EUR" "USD" "HRK"])}
        ui/enum-cell (rand-nth (get-in columns [7 :options]))
        ui/timestamp-cell (rand-date)
        ui/text-cell (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
        ui/boolean-cell (rand-nth [true false])
        nil))))

(defn generate-row
  "Will go through columns and for each collumn call generate-column
  function."
  []
  (reduce
   (fn [r {c :cursor :as column}]
     (assoc r c (generate-column column)))
   {}
   columns))

(defn generate-table
  "Genearte \"cnt\" number of rows"
  [cnt]
  (loop [c cnt
         r []]
    (if (zero? c) r
        (recur (dec c) (conj r (assoc (generate-row) :idx (count r)))))))

(def data (generate-table 50))

(defn reducer
  [{:keys [data] :as state}
    ;;
   {:keys [type idx value]
    {:keys [cursor]
     cidx :idx} :column}]
  (letfn [(apply-filters
            [{:keys [rows columns] :as state}]
            (if-some [filters (not-empty
                               (keep
                                (fn [{f :filter c :cursor}]
                                  (when f
                                    (case c
                                      :timestamp
                                      (let [[from to] f]
                                        (fn [{t :timestamp}]
                                          (cond
                                            (every? some? [from to]) (<= from t to)
                                            (some? from) (<= from t)
                                            (some? to) (<= t to))))
                                      :enum (comp f :enum)
                                      (constantly true))))
                                columns))]
              (assoc state :data (filter (apply every-pred filters) rows))
              (assoc state :data rows)))]
    (let [cursor' (if (sequential? cursor) cursor
                      [cursor])]
      (->
       (case type
         :table.element/change
         (assoc-in state (into [:rows (:idx (nth data idx))] cursor') value)
         :table.column/filter
         (assoc-in state [:columns cidx :filter] value)

         state)
            ;;
       apply-filters))))

(defnc extended-row
  []
  (d/div "hello from extended row"))

(defnc custom-cell
  []
  (let [{:keys [cursor]} (table/use-column)]
    (d/div
     {:style
      {:display "flex"
       :flex-grow "1"
       :background-color (case cursor
                           :b "red"
                           :c "yellow"
                           :d "green"
                           "black")}}
     \u00A0)))

(defnc custom-row
  {:wrap [(ui/forward-ref)]}
  [props _ref]
  ($ table/Row
     {:ref _ref
      :className "trow"
      & (dissoc props :className :class)}
     ($ extended-row)))

(def row-example-columns
  [{:cursor :a
    :cell custom-cell
    :width 10
    :name "Column 1"}
   {:cursor :b
    :width 5
    :cell custom-cell
    :name "Column 2"}
   {:cursor :c
    :width 5
    :cell custom-cell
    :name "Column 3"}])

(defnc
  row-example
  {:wrap [(ui/forward-ref)
          (provider/extend-ui
           #:table {:row custom-row})]}
  []
  (let []
    ($ layout/Container
       {:style
        {:width 500
         :height 42}}
       ($ ui/table
          {:columns row-example-columns
           :rows [{:a 1 :b 2 :c 3 :d 4}]}))))

(defnc table-example
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)
        [{:keys [data columns]} dispatch] (hooks/use-reducer
                                           reducer
                                           {:rows data
                                            :data data
                                            :columns columns})]
    ($ layout/Container
       {:style {:width width
                :height "500px"}}
       ($ ui/table
          {:rows data
           :columns columns
           :dispatch dispatch}))))

(defnc Table
  {:wrap [(router/wrap-rendered :toddler.table)
          (router/wrap-link
           :toddler.table
           [{:id ::intro
             :name "Intro"
             :hash "in-general"}
            {:id ::extend
             :name "Expand Example"
             :hash "expand-example"}
            {:id ::dnd
             :name "Drag'n drop"
             :hash "dnd-example"}])]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "45rem"}
              :className (css
                          ["& .example-field" :my-5]
                          ["& #toddler-table-example" :my-10])}
             ($ md/watch-url {:url "/doc/en/tables.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "row-example")}
                ($ row-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "toddler-table-example")}
                ($ table-example)))))))

(let [large [{:i "top" :x 0 :y 0 :w 10 :h 1}
             {:i "bottom-left" :x 0 :y 1 :w 5 :h 1}
             {:i "bottom-right" :x 5 :y 1 :w 5 :h 1}]
      small [{:i "top" :x 0 :y 0 :w 1 :h 1}
             {:i "bottom-left" :x 0 :y 1 :w 1 :h 1}
             {:i "bottom-right" :x 0 :y 2 :w 1 :h 1}]
      layouts {:md large
               :lg large
               :sm small
               :xs small}
      grid-columns {:lg 10 :md 10 :sm 1 :xs 1}]
  (defnc TableGrid
    {:wrap [(router/wrap-rendered :toddler.multi-tables)]}
    []
    (let [{:keys [height width]} (layout/use-container-dimensions)]
      ($ ui/simplebar
         {:style {:height height
                  :width width}}
         ($ grid/GridLayout
            {:width width
             :row-height (/ height 2)
             :columns grid-columns
             :layouts layouts}
            ($ ui/table
               {:key "top"
                :rows data
                :columns columns
                :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))})
            ($ ui/table
               {:key "bottom-left"
                :rows data
                :columns columns
                :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))})
            ($ ui/table
               {:key "bottom-right"
                :rows data
                :columns columns
                :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))}))))))
