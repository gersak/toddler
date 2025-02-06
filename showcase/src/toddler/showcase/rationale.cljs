(ns toddler.showcase.rationale
  (:require
   [helix.core :refer [$ defnc]]
   [shadow.css :refer [css]]
   [toddler.core :as toddler]
   [toddler.ui :as ui :refer [!]]
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
    (! :simplebar {:style {:height height
                           :width width}}
       (! :row {:align :center}
          (! :column {:align :center
                      :style {:max-width "40rem"}
                      :className (css
                                  ["& .component" :my-6])}
             ($ md/watch-url {:url "/rationale.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "components-example")}
                ($ MyApp)))))))
