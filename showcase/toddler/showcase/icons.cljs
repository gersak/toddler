(ns toddler.showcase.icons
  (:require
   [helix.core :refer [defnc $ <>]]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.core :as toddler]
   [toddler.layout :as layout]
   [toddler.ui :as ui]
   [toddler.md.lazy :as md]
   [toddler.router :as router]
   [toddler.fav6.solid :as solid]
   [toddler.showcase.icons.material :as material]
   [toddler.showcase.icons.fav6 :as fav6]
   [toddler.showcase.icons.ionic :as ionic]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 #:showcase.icons {:default "Icons"
                   :hr "Ikone"})

(declare outlined)

(defnc display-icons
  [{:keys [height icons]}]
  ($ ui/simplebar
     {:style {:height (- height 80)}
      :className (css :pt-4)
      :shadow true}
     (d/div
      {:className (css
                   :flex :flex-wrap
                   ["& .icon-wrapper" :m-4 :flex :flex-col :justify-center :items-center]
                   ["& .icon" {:font-size "24px"} :cursor-pointer]
                   ["& .name" :font-semibold {:font-size "10px"}])}
      (map
       (fn [[name icon]]
         ($ ui/tooltip
            {:key name
             :message (d/div {:className "name"} name)}
            (d/div
             {:className "icon-wrapper"}
             ($ icon {:className "icon"
                      :onClick (fn [] (.writeText js/navigator.clipboard name))}))))
       icons))))

(defnc Icons
  []
  {:wrap [(router/wrap-rendered :toddler.icons)]}
  (let [{:keys [height]} (layout/use-container-dimensions)
        [message {message-height :height}] (toddler/use-dimensions)
        height (- height message-height)
        opened (router/use-rendered? ::info)
        open-modal (router/use-go-to ::info)
        close-modal (router/use-go-to :toddler.icons)]
    (router/use-link
     :toddler.icons
     [{:id ::info
       :segment "info"}])
    (d/div
     {:className (css :flex :flex-col)}
     (d/div
      {:ref #(reset! message %)
       :className (css
                   :relative
                   :flex
                   :flex-col
                   :text-xs
                   :pl-20
                   :pr-10
                   :mb-2
                   {:max-width "700px"}
                   ["& p" :my-1])}
      (d/p "Toddler generates helix components by cloning target icons repo and than processing source code. Why?")
      (d/p "Because of " (d/b "advanced compilation") " and dead code elimination of Closure compiler.")
      (d/div
       {:className (css :absolute ["& svg" :cursor-pointer {:font-size "24px"}])
        :style {:top 3 :right 10}}
       ($ solid/circle-info
          {:on-click #(open-modal)})))
     (when opened
       ($ ui/modal-dialog
          {:on-close #(close-modal)
           :className (css {:max-width "700px"})}
          (d/div {:className "title"} "How?")
          (d/div
           {:className "content"}
           ($ ui/simplebar
              {:style {:max-height (- height 100)
                       :min-width 600}}
              ($ md/watch-url {:url "/icons.md"})))
          (d/div
           {:className "footer"})))
     ($ ui/tabs
        ($ ui/tab
           {:id ::material-outlined
            :name "Material Outlined"}
           ($ display-icons {:height height :icons material/outlined}))
        ($ ui/tab
           {:id ::material-round
            :name "Material Round"}
           ($ display-icons {:height height :icons material/round}))
        ($ ui/tab
           {:id ::material-sharp
            :name "Material Sharp"}
           ($ display-icons {:height height :icons material/sharp}))
        ($ ui/tab
           {:id ::fav6-regular
            :name "FA Regular"}
           ($ display-icons {:height height :icons fav6/regular}))
        ($ ui/tab
           {:id ::fav6-solid
            :name "FA Solid"}
           ($ display-icons {:height height :icons fav6/solid}))
        ($ ui/tab
           {:id ::fav6-brands
            :name "FA Brands"}
           ($ display-icons {:height height :icons fav6/brands}))
        ($ ui/tab
           {:id ::ionic
            :name "Ionic"}
           ($ display-icons {:height height :icons ionic/icons}))))))
