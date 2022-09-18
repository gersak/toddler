(ns toddler.showcase.table
   (:require
      [toddler.dev :as dev]
      [toddler.elements :as toddler]
      [toddler.elements.table :as table]
      [toddler.grid :as grid]
      toddler.elements.table.theme
      [vura.core :as vura]
      [helix.core :refer [$ defnc]]
      [helix.dom :as d]
      [helix.styled-components :refer [defstyled]]
      [helix.children :as c]))


(def columns
   [{:cursor :euuid
     :label "UUID"
     :type "uuid"
     :style {:width 50}}
    {:cursor :user
     :label "User"
     :type "user"
     :style {:width 100}}
    {:cursor :integer
     :type "int"
     :label "Integer"
     :style {:width 100}}
    {:cursor :text
     :type "string"
     :label "Text"
     :style {:width 250}}
    {:cursor :enum
     :label "ENUM"
     :type "enum"
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
     :label "Timestamp"
     :type "timestamp"
     :style {:width 120}}
    {:cursor :boolean
     :label "BOOL"
     :type "boolean"
     :style {:width 50}}])


(defn generate-column
   [{t :type}]
   (let [now (-> (vura/date) vura/time->value)]
      (letfn [(rand-date
                 []
                 (->
                    now
                    (+ (* (rand-nth [1 -1])
                          (vura/hours (rand-int 1000)))
                       (vura/minutes (rand-int 60)))
                    vura/value->time))]
         (case t
            "uuid" (random-uuid)
            "int" (rand-int 10000)
            "user" {:euuid (random-uuid)
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
            "enum" (:value (rand-nth (get-in columns [4 :options])))
            "timestamp" (rand-date)
            "string" (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
            "bool" (rand-nth [true false])
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
         (recur (dec c) (conj r (generate-row))))))


(comment
   (generate-table 100))


(def data (generate-table 50))


(defnc TableContainer
   [{:keys [style] :as props}]
   ($ toddler/Container
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

(defnc Table
   []
   (d/div
      {:style
       {:width "100%" :height "100%" ;:width width :height height
        :display "flex"
        :padding 30
        :box-sizing "border-box"
        :justifyContent "center"
        :alignItems "center"}}
      ($ TableContainer
         ($ table/table
            {:rows data
             :columns columns
             :dispatch (fn [event]
                          (println "Dispatching\n" event))}))))


(dev/add-component
   {:key ::table
    :name "Table"
    :render Table})

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
      (let [{:keys [height width]} (toddler/use-container-dimensions)]
         ; (println "DIMS: " dims)
         ; (println "HH: " half-height)
         ; (println "HW: " half-width)
         ($ toddler/simplebar
            {:style #js {:height height
                         :width width
                         :boxSizing "border-box"}}
            ($ grid/GridLayout
               {:width width
                :row-height (/ height 2)
                :columns grid-columns
                :layouts layouts}
               ($ table/table
                  {:key "top"
                   :rows data
                   :columns columns
                   :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))})
               ($ table/table
                  {:key "bottom-left"
                   :rows data
                   :columns columns
                   :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))})
               ($ table/table
                  {:key "bottom-right"
                   :rows data
                   :columns columns
                   :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))}))))))

; (defnc TableGrid
;    []
;    (let [{:keys [height width] :as dims} (toddler/use-container-dimensions)
;          half-height (/ height 2)
;          half-width (/ width 2)]
;       ; (println "DIMS: " dims)
;       ; (println "HH: " half-height)
;       ; (println "HW: " half-width)
;       ($ toddler/Column
;          {:style {:height height
;                   :width width
;                   :boxSizing "border-box"}}
;          ($ toddler/Row
;             {:style {:padding 10}}
;             ($ table/table
;                {:rows data
;                 :columns columns
;                 :dispatch (fn [event]
;                              (println "Dispatching\n" event))}))
;          ($ toddler/Row
;             {:style (cond->
;                        {:padding 10}
;                        (< half-width 500) (assoc :flexWrap "wrap"))}
;             ($ table/table
;                {:rows data
;                 :columns columns
;                 :dispatch (fn [event]
;                              (println "Dispatching\n" event))})
;             ($ table/table
;                {:rows data
;                 :columns columns
;                 :dispatch (fn [event]
;                              (println "Dispatching\n" event))})))))


(dev/add-component
   {:key ::tables
    :name "Multiple Tables"
    :render TableGrid})
