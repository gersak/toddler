(ns toddler.showcase.layout
  (:require
   [helix.core :refer [$ defnc <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.md.lazy :as md]
   [toddler.ui :as ui :refer [!]]
   [toddler.core :as toddler]
   [toddler.layout :as layout]
   [toddler.router :as router]))

(defnc rows-columns-example
  []
  (let [[row set-row!] (hooks/use-state :explode)
        [column set-column!] (hooks/use-state :explode)]
    (<>
     ($ ui/row
        {:align row
         :className (css
                     :bg-normal+ :border :border-normal
                     :mt-4
                     {:border-radius "50px"}
                     ["& .element"
                      :bg-normal- :border :border-normal+
                      {:min-height "50px"
                       :max-width "50px"
                       :border-radius "50px"}])}
        ($ ui/column {:className "element"})
        ($ ui/column {:className "element"})
        ($ ui/column {:className "element"}))
     ($ ui/row {:align :center
                :className (css :mt-4)}
        ($ ui/dropdown-field
           {:name "Row Alignment"
            :options [:start :center :end :explode]
            :search-fn name
            :on-change set-row!
            :value row}))
     ($ ui/row {:align :center
                :className (css :mt-4)}
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
        {:className (css :mt-5)}
        ($ ui/dropdown-field
           {:name "Column Alignment"
            :options [:start :center :end :explode]
            :search-fn name
            :on-change set-column!
            :value column})))))

(defnc Layout
  {:wrap [(router/wrap-rendered :toddler.layout)
          (router/wrap-link
           :toddler.layout
           [{:id ::rows_columns
             :name "Rows & Columns"
             :hash "rows&columns"}
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
                ($ rows-columns-example)))))))
