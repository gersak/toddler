(ns toddler.showcase.table
   (:require
      [toddler.dev :as dev]
      [toddler.elements :as toddler]
      [toddler.elements.table :as table]
      [vura.core :as vura]
      [helix.core :refer [$ defnc]]
      [helix.hooks :as hooks]))


(def columns
   [{:cursor :euuid
     :label "UUID"
     :type "uuid"}
    {:cursor :user
     :label "User"
     :type "user"}
    {:cursor :integer
     :type "int"
     :label "Integer"}
    {:cursor :text
     :type "string"
     :label "Text"}
    {:cursor :enum
     :label "ENUM"
     :type "enum"}
    {:cursor :timestamp
     :label "Timestamp"
     :type "timestamp"}
    {:cursor :boolean
     :label "BOOL"
     :type "boolean"}])


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
            "timestamp" (rand-date)
            "string" (apply str (repeatedly 200 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
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


(def data (generate-table 1))


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
