(ns toddler.showcase.table
   (:require
      [toddler.dev :as dev]
      [toddler.dev.context
       :refer [*header*
               *navbar*]]
      [toddler.elements :as toddler]
      [toddler.elements.table :as table]
      toddler.elements.table.theme
      [vura.core :as vura]
      [helix.core :refer [$ defnc]]))


(def columns
   [{:cursor :euuid
     :label "UUID"
     :type "uuid"
     :style {:width 30}}
    {:cursor :user
     :label "User"
     :type "user"
     :style {:width 50}}
    {:cursor :integer
     :type "int"
     :label "Integer"
     :style {:width 50}}
    {:cursor :text
     :type "string"
     :label "Text"
     :style {:width 150}}
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
     :style {:width 50}}
    {:cursor :timestamp
     :label "Timestamp"
     :type "timestamp"
     :style {:width 80}}
    {:cursor :boolean
     :label "BOOL"
     :type "boolean"
     :style {:width 30}}])


(defn generate-column
   [{t :type}]
   (let [now (-> (vura/date) vura/time->value)]
      (letfn [(rand-date
                 []
                 (->
                    now
                    (+ (* (rand-nth [1 -1])
                          (rand-int 1000)))
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
            "string" (apply str (repeatedly 240 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
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


(def data (generate-table 100))


; (defnc Table
;    []
;    (let [{:keys [width height]} (toddler/use-parent-container-dimensions)]
;       (println "TABL: " [width height])
;       ($ toddler/Container
;          {:style {:width (- width 10)
;                   :height (- height 10)}
;           :display "flex"
;           :justifyContent "center"
;           :align-items "center"}
;          ($ table/table
;             {:rows data
;              :columns columns
;              :dispatch (fn [event]
;                           (println "Dispatching\n" event))}))))


(defnc Table
   []
   ($ table/table
      {:rows data
       :columns columns
       :dispatch (fn [event]
                    (println "Dispatching\n" event))}))


(dev/add-component
   {:key ::period-input
    :name "Table"
    :render Table})
