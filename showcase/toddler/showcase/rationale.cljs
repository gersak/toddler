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
   [toddler.router :as router]
   [toddler.showcase.components :refer [MyApp]]))

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
             {:position :center
              :style {:max-width "40rem"}
              :className (css
                          ["& .component" :my-6])}
             ($ md/watch-url {:url "/rationale.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "components-example")}
                ($ MyApp)))))))
