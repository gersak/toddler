(ns toddler.showcase.table
  (:require
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.grid :as grid]
   [toddler.ui :as ui :refer [!]]
   [toddler.table :as table]
   [toddler.core :as toddler]
   [toddler.router :as router]
   [toddler.md.lazy :as md]
   [vura.core :as vura]
   [helix.core :refer [$ defnc provider]]
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
    :cell :cell/uuid
    :width 50}
   {:cursor :user
    :label "User"
    :cell :cell/identity
    :options (repeatedly 3 random-user)
    :width 100}
   {:cursor :float
    :cell :cell/float
    :label "Float"
    :width 100}
   {:cursor :integer
    :cell :cell/integer
    :label "Integer"
    :width 100}
   {:cursor :text
    :cell :cell/text
    :label "Text"
    :width 250}
   {:cursor :enum
    :label "ENUM"
    :cell :cell/enum
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
    :cell :cell/timestamp
    :label "Timestamp"
    :show-time false
    :width 120}
   {:cursor :boolean
    :cell  :cell/boolean
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
        :cell/uuid (random-uuid)
        :cell/integer (rand-int 10000)
        :cell/float (* (rand) 1000)
        :cell/identity (random-user)
        :cell/currency {:amount (vura/round-number (* 1000 (rand)) 0.25)
                        :currency (rand-nth ["EUR" "USD" "HRK"])}
        :cell/enum (rand-nth (get-in columns [5 :options]))
        :cell/timestamp (rand-date)
        :cell/text (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))
        :cell/boolean (rand-nth [true false])
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
    ($ ui/row
       {:align :center}
       (provider
        {:context layout/*container-dimensions*
         :value {:width 600
                 :height 500}}
        (! :table
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
      (! :row
         (d/label "Movie")
         (d/div movie))
      (! :row
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
    :label "Character"}])

(defnc row-example
  {:wrap [(ui/forward-ref)
          (ui/extend-ui
           #:table {:row custom-row})]}
  []
  (let [[state set-state!] (hooks/use-state expand-data)]
    (! :row {:align :center}
       ($ layout/Container
          {:style
           {:width 500
            :height 400}}
          (! :table
             {:columns row-example-columns
              :dispatch (fn [{:keys [type value idx] :as evt
                              {:keys [cursor]} :column}]
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
    (! :simplebar
       {:style {:height height
                :width width}}
       (! :row {:align :center}
          (! :column
             {:align :center
              :className (css
                          :items-center
                          ["& .example-field" :my-5]
                          ["& .toddler-markdown" {:max-width "40rem"}]
                          ["& #toddler-table-example" :my-10])}
             ($ md/watch-url {:url "/tables.md"})
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
      (! :simplebar
         {:style {:height height
                  :width width}}
         ($ grid/GridLayout
            {:width width
             :row-height (/ height 2)
             :columns grid-columns
             :layouts layouts}
            (! :table
               {:key "top"
                :rows data
                :columns columns
                :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))})
            (! :table
               {:key "bottom-left"
                :rows data
                :columns columns
                :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))})
            (! :table
               {:key "bottom-right"
                :rows data
                :columns columns
                :dispatch (fn [evnt] (println "Dispatching:\n%s" evnt))}))))))
