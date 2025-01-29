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
   [helix.children :as c]
   [toddler.material.outlined :as outlined]))

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
  [{:cursor :euuid
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
    :align :center
    :label "Boolean"
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
        ui/enum-cell (rand-nth (get-in columns [6 :options]))
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

(defnc table-example
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)
        [{:keys [data columns]} dispatch] (hooks/use-reducer
                                           reducer
                                           {:rows data
                                            :data data
                                            :columns columns})]
    ($ layout/Container
       {:style {:width (- width 80)
                :height "500px"}}
       ($ ui/row
          {:align :center}
          ($ ui/table
             {:rows data
              :columns columns
              :dispatch dispatch})))))

(def expand-data
  [{:first-name "Donnie" :last-name "Darko" :gender :male
    :movie "Donnie Darko, 2001"
    :description "Played by Jake Gyllenhaal, Donnie is a troubled teenager experiencing time loops, apocalyptic visions, and cryptic messages from a man in a rabbit suit."}
   {:first-name "Ellen" :last-name "Rippley" :gender :female
    :movie "Alien (1979) & Aliens (1986)"
    :description "Played by Guy Pearce, Leonard suffers from short-term memory loss and relies on tattoos and notes to track his search for his wife’s killer"}
   {:first-name "Sarah" :last-name "Connor" :gender :female
    :movie "The Terminator (1984) & Terminator 2: Judgment Day (1991)"
    :description "Played by Linda Hamilton, Sarah evolves from a terrified waitress to a hardened warrior, fighting to protect her son and prevent Judgment Day."}
   {:first-name "Leonard" :last-name "Shelby" :gender :male
    :movie "Memento, 2000"
    :description "Played by Guy Pearce, Leonard suffers from short-term memory loss and relies on tattoos and notes to track his search for his wife’s killer."}
   {:first-name "Roy" :last-name "Batty" :gender :male
    :movie "Blade Runner, 1982"
    :description "Played by Rutger Hauer, Roy is a rogue replicant searching for more life, delivering one of sci-fi’s most poetic monologues before his tragic end."}
   {:first-name "Furiosa" :gender :female
    :movie "Mad Max: Fury Road (2015)"
    :description "Played by Charlize Theron, Furiosa is a fearless, battle-hardened warrior fighting for freedom in a dystopian wasteland."}])

(defnc expand-cell
  []
  (let [[value set-value!] (table/use-cell-state)]
    (d/div
     {:className (css :flex :flex-grow :items-center :justify-center :cursor-pointer)
      :on-click #(set-value! (not value))}
     ($ (if value outlined/keyboard-arrow-up outlined/keyboard-arrow-down)
        {:className (css {:font-size "24px"})}))))

(defnc custom-cell
  []
  (let [{:keys [first-name last-name]} (table/use-row)]
    (d/div (str first-name " " last-name))))

(defnc extended-row
  []
  (let [{{:keys [expanded]} :ui
         :keys [movie description]} (table/use-row)
        [el {:keys [height]}] (toddler/use-dimensions)]
    (d/div
     {:className (css
                  :overflow-hidden
                  :text-xs
                  {:transition "height .2s ease-in-out"}
                  ["& label" :font-semibold :color+ :ml-4 {:min-width "100px"}])
      :style {:height (if expanded height 0)}}
     (d/div
      {:ref #(reset! el %)}
      ($ ui/row
         (d/label "Movie")
         (d/div movie))
      ($ ui/row
         (d/label "Description")
         (d/div description))))))

(defnc custom-row
  {:wrap [(ui/forward-ref)]}
  [props _ref]
  ($ table/Row
     {:ref _ref
      :className "trow"
      & (dissoc props :className :class)}
     ($ extended-row)))

(def row-example-columns
  [{:cursor [:ui :expanded]
    :cell expand-cell
    :width 32
    :align #{:center}
    :style {:max-width 48}}
   {:cell custom-cell
    :width 140
    :align #{:center :left}
    :label "Character"
    :header ui/plain-header}])

(defnc row-example
  {:wrap [(ui/forward-ref)
          (provider/extend-ui
           #:table {:row custom-row})]}
  []
  (let [[state set-state!] (hooks/use-state expand-data)]
    ($ ui/row
       {:align :center}
       ($ layout/Container
          {:style
           {:width 500
            :height 400}}
          ($ ui/table
             {:columns row-example-columns
              :dispatch (fn [{:keys [type value idx] :as evt
                              {:keys [cursor]} :column}]
                          (println "WHRER: " evt)
                          (case type
                            :table.element/change (set-state! assoc-in (concat [idx] cursor) value)
                            (.error js/console "Unkown event: " (pr-str evt))))
              :rows state})))))

;; TODO - include this examples as well
(defnc Table
  {:wrap [(router/wrap-rendered :toddler.table)
          (router/wrap-link
           :toddler.table
           [{:id ::intro
             :name "Intro"
             :hash "in-general"}
            {:id ::demo
             :name "Demo"
             :hash "demo"}
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
              :style {:max-width "40rem"}
              :className (css
                          ["& .example-field" :my-5]
                          ["& #toddler-table-example" :my-10])}
             ($ md/watch-url {:url "/tables.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "row-example")}
                ($ row-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "toddler-table-example")}
                ($ table-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "expand-row-example")}
                ($ row-example)))))))

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
