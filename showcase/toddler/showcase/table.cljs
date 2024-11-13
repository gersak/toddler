(ns toddler.showcase.table
   (:require
      [toddler.dev :as dev]
      [toddler.layout :as layout]
      [toddler.grid :as grid]
      [toddler.ui :as ui]
      [toddler.ui.components :as components]
      [vura.core :as vura]
      [helix.core :refer [$ defnc]]
      [helix.dom :as d]
      [helix.hooks :as hooks]
      [helix.children :as c]
      [toddler.i18n.keyword :refer [add-translations]]))



(add-translations
   (merge
      #:showcase.table {:default "Table"
                        :hr "Tablica"}
      #:showcase.multi-tables {:default "Multi Table"
                               :hr "Više tablica"}))


(def columns
   [{:cursor [:ui :expand]
     :cell ui/expand-cell
     :style {:width 20}}
    {:cursor :euuid
     :label "UUID"
     :align :center
     :cell ui/uuid-cell
     :style {:width 50}}
    {:cursor :user
     :label "User"
     :header ui/plain-header
     :cell ui/identity-cell
     :style {:width 100}}
    {:cursor :float
     :header ui/plain-header
     :cell ui/float-cell
     :label "Float"
     :style {:width 100}}
    {:cursor :integer
     :header ui/plain-header
     :cell ui/integer-cell
     :label "Integer"
     :style {:width 100}}
    {:cursor :text
     :header ui/text-header
     :cell ui/text-cell
     :label "Text"
     :style {:width 250}}
    {:cursor :currency
     :header ui/currency-header
     :cell ui/currency-cell
     :style {:width 150}
     :label "Money"}
    {:cursor :enum
     :label "ENUM"
     :cell ui/enum-cell
     :header ui/enum-header
     :options [{:name "Dog"
                :value :dog}
               {:name "Cat"
                :value :cat}
               {:name "Horse"
                :value :horse}
               {:name "Hippopotamus"
                :value :hypo}]
     :placeholder "Choose your fav"
     :style {:width 100}}
    {:cursor :timestamp
     :cell ui/timestamp-cell
     :header ui/timestamp-header
     :label "Timestamp"
     :show-time false
     :style {:width 120}}
    {:cursor :boolean
     :cell ui/boolean-cell
     :header ui/boolean-header
     :label "Boolean"
     :type "boolean"
     :style {:width 50}}])


(defn generate-column
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
            ui/identity-cell {:euuid (random-uuid)
                              :name (rand-nth
                                       ["John"
                                        "Emerick"
                                        "Harry"
                                        "Ivan"
                                        "Dugi"
                                        "Ricky"])
                              :avatar (rand-nth
                                         ["https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse1.mm.bing.net%2Fth%3Fid%3DOIP.FMXcWvy8DeSem2kV_8KH0gHaEK%26pid%3DApi&f=1"
                                          "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse1.mm.bing.net%2Fth%3Fid%3DOIP.SuUOaB0bwigOLr3NLT2ZZgHaEK%26pid%3DApi&f=1"
                                          "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse4.mm.bing.net%2Fth%3Fid%3DOIP.VCtUu6tnkPzLht6T46WD5wHaEx%26pid%3DApi&f=1"
                                          "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse2.mm.bing.net%2Fth%3Fid%3DOIP.3JCqIfj_9yEyfPeWvNwdeQHaDt%26pid%3DApi&f=1"
                                          "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse4.mm.bing.net%2Fth%3Fid%3DOIP.jUhREZmYLBkJCe7cmSdevwHaEX%26pid%3DApi&f=1"
                                          "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse2.mm.bing.net%2Fth%3Fid%3DOIP.w2kZvvrVVyFG0JNVzdYhbwHaEK%26pid%3DApi&f=1"
                                          "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse4.mm.bing.net%2Fth%3Fid%3DOIP.iBbhCR5cHpgkHsABbNeVtQHaEK%26pid%3DApi&f=1"])}
            ui/currency-cell {:amount (vura/round-number (* 1000 (rand)) 0.25)
                              :currency (rand-nth ["EUR" "USD" "HRK"])}
            ui/enum-cell (rand-nth (get-in columns [7 :options]))
            ui/timestamp-cell (rand-date)
            ui/text-cell (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
            ui/boolean-cell (rand-nth [true false])
            nil))))


(defn generate-row
   []
   (reduce
      (fn [r {c :cursor :as column}]
         (assoc r c (generate-column column)))
      {}
      columns))


(defn generate-table
   [cnt]
   (loop [c cnt
          r []]
      (if (zero? c) r
         (recur (dec c) (conj r (assoc (generate-row) :idx (count r)))))))


(comment
   (generate-table 100))


(def data (generate-table 50))


(defnc TableContainer
   [{:keys [style] :as props}]
   ($ layout/Container
      {:style
       (merge
          {:display "flex"
           :flex-grow "1"
           :width "100%"
           :height "100%"
           :padding 10
           :box-sizing "border-box"
           :justifyContent "center"
           :align-content "center"}
          style)}
      (c/children props)))


(defn reducer
   [{:keys [data] :as state}
    ;;
    {:keys [type idx value]
     {:keys [cursor]
      cidx :idx} :column
     :as evnt}]
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


(defnc Table
   []
   ($ components/Provider
      (d/div
         {:style
          {:width "100%" :height "100%"
           :display "flex"
           :padding 30
           :box-sizing "border-box"
           :justifyContent "center"
           :alignItems "center"}}
         (let [[{:keys [data columns]} dispatch] (hooks/use-reducer
                                                    reducer
                                                    {:rows data
                                                     :data data
                                                     :columns columns})]
            ($ TableContainer
               ($ ui/table
                  {:rows data
                   :columns columns
                   :dispatch dispatch}))))))


; (dev/add-component
;    {:key ::table
;     :name "Table"
;     :render Table})


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


; (dev/add-component
;    {:key ::tables
;     :name "Multiple Tables"
;     :render TableGrid})
