(ns toddler.showcase.rationale
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.core :as toddler]
   [toddler.ui :as ui]
   [toddler.layout :as layout]
   [toddler.md.lazy :as md]
   [toddler.router :as router]))

(defnc Rationale
  {:wrap [(router/wrap-link
           :toddler.rationale
           [{:id ::concepts
             :hash "concepts"}
            {:id ::future
             :hash "future"}])
          (router/wrap-rendered :toddler.rationale)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "30rem"}
              :className (css
                          ["& .component" :my-6])}
             ($ md/watch-url {:url "/rationale.md"})
             #_($ toddler/portal
                  {:locator #(.getElementById js/document "calendar-period-example")}
                  ($ calendar-period)))))))
