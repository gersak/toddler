(ns toddler.showcase.layout
  (:require
   [helix.core :refer [$ defnc <> provider]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.md.lazy :as md]
   [toddler.ui :as ui]
   [toddler.core :as toddler]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.showcase.table :as showcase.table]
   [toddler.showcase.modal :as showcase.modal]))

(defnc rows-columns-example
  []
  (let [[row set-row!] (hooks/use-state :explode)
        [column set-column!] (hooks/use-state :explode)]
    (<>
     ($ ui/column
        {:align "center"
         :className (css :items-center)}
        ($ ui/row
           {:align row
            :className (css
                        :bg-normal+ :border :border-normal
                        :mt-4

                        {:border-radius "50px"
                         :width "300px"}
                        ["& .element"
                         :bg-normal- :border :border-normal+
                         {:min-height "50px"
                          :max-width "50px"
                          :border-radius "50px"}])}
           ($ ui/column {:className "element"})
           ($ ui/column {:className "element"})
           ($ ui/column {:className "element"}))
        ($ ui/row {:align :center
                   :className (css :mt-4 {:width "300px"})}
           ($ ui/dropdown-field
              {:name "Row Alignment"
               :options [:start :center :end :explode]
               :search-fn name
               :on-change set-row!
               :value row})))
     ;;
     ($ ui/column
        {:className (css :items-center)}
        ($ ui/row {:align :center
                   :className (css :mt-16 {:width "300px"})}
           ($ ui/column
              {:align column
               :className (css
                           :bg-normal+ :border :border-normal
                           {:min-height "400px"
                            :border-radius "25px"}
                           ["& .element"
                            :bg-normal- :border :border-normal+
                            {:max-height "50px"
                             :border-radius "50px"}])}
              ($ ui/row {:className "element"})
              ($ ui/row {:className "element"})
              ($ ui/row {:className "element"})))
        ($ ui/row
           {:className (css :mt-5 {:width "300px"})}
           ($ ui/dropdown-field
              {:name "Column Alignment"
               :options [:start :center :end :explode]
               :search-fn name
               :on-change set-column!
               :value column}))))))

(def user-data
  [{:id 1,
    :name "Alice Johnson",
    :username "alicej",
    :email "alice@example.com",
    :role "User",
    :bio "Loves hiking and photography."}
   {:id 2,
    :name "Bob Smith",
    :username "bobsmith",
    :email "bob@example.com",
    :role "Admin",
    :bio "Full-stack developer and tech enthusiast."}
   {:id 3,
    :name "Charlie Brown",
    :username "charlieb",
    :email "charlie@example.com",
    :role "User",
    :bio "Passionate about AI and machine learning."}
   {:id 4,
    :name "Diana Prince",
    :username "wonderdiana",
    :email "diana@example.com",
    :role "Moderator",
    :bio "Helping the community grow!"}
   {:id 5,
    :name "Eve Adams",
    :username "evea",
    :email "eve@example.com",
    :role "User",
    :bio "Avid reader and coffee lover."}
   {:id 6,
    :name "Frank West",
    :username "frankw",
    :email "frank@example.com",
    :role "User",
    :bio "Gamer, developer, and open-source contributor."}
   {:id 7,
    :name "Grace Hopper",
    :username "graceh",
    :email "grace@example.com",
    :role "Admin",
    :bio "Pioneer in computer science."}
   {:id 8,
    :name "Henry Ford",
    :username "henryf",
    :email "henry@example.com",
    :role "User",
    :bio "Car enthusiast and industrial designer."}
   {:id 9,
    :name "Ivy Carter",
    :username "ivyc",
    :email "ivy@example.com",
    :role "User",
    :bio "Aspiring musician and songwriter."}
   {:id 10,
    :name "Jack Daniels",
    :username "jackd",
    :email "jack@example.com",
    :role "User",
    :bio "Whiskey lover and history buff."}
   {:id 11,
    :name "Katherine Johnson",
    :username "katjohnson",
    :email "katherine@example.com",
    :role "Moderator",
    :bio "Mathematician and space exploration enthusiast."}
   {:id 12,
    :name "Leo Nakamura",
    :username "leonak",
    :email "leo@example.com",
    :role "User",
    :bio "Passionate about robotics and automation."}
   {:id 13,
    :name "Mona Lisa",
    :username "monalisa",
    :email "mona@example.com",
    :role "User",
    :bio "Art lover and museum explorer."}
   {:id 14,
    :name "Noah Thompson",
    :username "noaht",
    :email "noah@example.com",
    :role "Admin",
    :bio "Enjoys working with databases and APIs."}
   {:id 15,
    :name "Olivia Martinez",
    :username "oliviam",
    :email "olivia@example.com",
    :role "User",
    :bio "Travel blogger and adventure seeker."}
   {:id 16,
    :name "Paul Anderson",
    :username "paula",
    :email "paul@example.com",
    :role "User",
    :bio "Backend developer and database expert."}
   {:id 17,
    :name "Quincy Roberts",
    :username "quincyr",
    :email "quincy@example.com",
    :role "User",
    :bio "Fitness coach and wellness advocate."}
   {:id 18,
    :name "Rachel Green",
    :username "rachelg",
    :email "rachel@example.com",
    :role "User",
    :bio "Fashion designer and trendsetter."}
   {:id 19,
    :name "Steve Rogers",
    :username "captainsteve",
    :email "steve@example.com",
    :role "Moderator",
    :bio "Loyal to justice and good ethics."}
   {:id 20,
    :name "Tina Turner",
    :username "tinat",
    :email "tina@example.com",
    :role "User",
    :bio "Music legend and performer."}])

(defnc user-item
  [{:keys [id name username email role bio]}]
  ($ ui/row
     {:className (css
                  :p-4
                  ["& .details img" :mt-3]
                  ["& .card" :p-2 :bg-normal+ :border :border-normal :rounded-md])}
     ($ ui/row
        {:className "card"
         :align :explode}
        ($ ui/row
           {:className "details"}
           ($ ui/avatar
              {:name name
               :size 48})
           ($ ui/column
              {:className (css
                           :ml-4
                           ["& .name" :font-semibold :text-sm]
                           ["& .info" :text-xxs :color :font-semibold]
                           ["& label" :color :font-normal :inline-block :mr-2 :text-xxxs])}
              (d/div {:className "name"} name)
              (d/div {:className "info"} (d/label "Username:") username)
              (d/div {:className "info"} (d/label "Email:") email)
              (d/div {:className "info"} (d/label "Role:") role)))
        ($ ui/column
           {:className (css
                        :self-start
                        :mt-1
                        {:max-width "200px"}
                        ["& label" :text-xxs :font-semibold :color-]
                        ["& p" :text-xxs])}
           (d/label "BIO")
           (d/p bio)))))

(defnc user-list
  []
  ($ ui/row
     {:align :center
      :className (css :mt-8)}
     ($ ui/simplebar
        {:style {:width 600
                 :height 600}}
        (map
         (fn [{:keys [id] :as data}]
           ($ user-item {:key id & data}))
         user-data))))

(defnc user-list-tab
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)]
    ($ ui/tab
       {:id ::user-list
        :name "Users"}
       ($ ui/row
          {:align :center}
          ($ ui/simplebar
             {:style {:width width
                      :height height}}
             (map
              (fn [{:keys [id] :as data}]
                ($ user-item {:key id & data}))
              user-data))))))

(defnc table-tab
  []
  ($ ui/tab {:id ::table
             :name "Table"}
     ($ ui/table {:rows showcase.table/data
                  :columns showcase.table/columns})))

(defnc tabs-example
  []
  (let [{:keys [width height]} (layout/use-container-dimensions)
        width (- width 200)]
    ($ ui/row
       {:align :center}
       (d/div
        {:style {:width width :height 400}
         :className (css :mb-4)}
        (provider
         {:context layout/*container-dimensions*
          :value {:width width
                  :height (min height 400)}}
         ($ ui/tabs
            {:className (css :mt-2)}
            ($ user-list-tab)
            ($ table-tab)
            ($ showcase.modal/form-tab)))))))

(defnc grid-example
  []
  (let [[[width height] set-dimensions!] (hooks/use-state [400 400])
        columns {:lg 4 :md 3 :sm 1}
        breakpoints {:sm 100
                     :md 399
                     :lg 500}
        layouts {:sm [{:i "one" :x 0 :y 0 :w 1 :h 1}
                      {:i "two" :x 0 :y 1 :w 1 :h 1}
                      {:i "three" :x 0 :y 2 :w 1 :h 1}
                      {:i "four" :x 0 :y 3 :w 1 :h 1}]
                 :md [{:i "one" :x 1 :y 0 :w 2 :h 1}
                      {:i "two" :x 0 :y 1 :w 3 :h 1}
                      {:i "three" :x 0 :y 0 :w 1 :h 1}
                      {:i "four" :x 0 :y 2 :w 3 :h 2}]
                 :lg [{:i "one" :x 0 :y 0 :w 3 :h 1}
                      {:i "two" :x 3 :y 0 :w 1 :h 2}
                      {:i "three" :x 0 :y 1 :w 3 :h 1}
                      {:i "four" :x 0 :y 2 :w 4 :h 2}]}]
    (<>
     ($ ui/row
        {:align :center}
        ($ ui/row
           {:className (css :my-4 {:max-width "200px"})}
           ($ ui/dropdown-field
              {:value width
               :name "Choose width"
               :on-change #(set-dimensions! assoc 0 %)
               :options [300 400 600]})))
     ($ ui/row
        {:align :center}
        (let [$box (css :flex :w-full :h-full
                        :justify-center :items-center
                        :font-bold :text-xl)]
          ($ ui/grid
             {:width width
              :height height
              :margin [5 5]
              :columns columns
              :layouts layouts
              :breakpoints breakpoints
              :row-height 100}
             (d/div {:key "one" :class [(css :bg-red-500) $box]} "ONE")
             (d/div {:key "two" :class [(css :bg-green-500) $box]} "TWO")
             (d/div {:key "three" :class [(css :bg-yellow-500) $box]} "THREE")
             (d/div {:key "four" :class [(css :bg-cyan-500) $box]} "FOUR")))))))

(defnc Layout
  {:wrap [(router/wrap-rendered :toddler.layout)
          (router/wrap-link
           :toddler.layout
           [{:id ::core
             :name "In General"
             :hash "core"}
            {:id ::rows_columns
             :name "Rows & Columns"
             :hash "rows-and-columns"}
            {:id ::tabs
             :name "Tabs"
             :hash "tabs"}
            {:id ::grid
             :name "Grid"
             :hash "grid"}])]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:style {:width "40rem"}
              :className (css
                          ["& .component" :my-6])}
             ($ md/watch-url {:url "/layout.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "rows-columns-example")}
                ($ rows-columns-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "list-example")}
                ($ user-list))
             ($ toddler/portal
                {:locator #(.getElementById js/document "tabs-example")}
                ($ tabs-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "grid-example")}
                ($ grid-example)))))))
