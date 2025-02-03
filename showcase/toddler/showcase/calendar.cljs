(ns toddler.showcase.calendar
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   ; [helix.dom :as d]
   [toddler.ui :as ui :refer [!]]
   [toddler.core :as toddler]
   [toddler.date :as date]
   [toddler.router :as router]
   [toddler.md.lazy :as md]
   [toddler.layout :as layout]))

(defnc month
  []
  (let [[{:keys [days]}] (date/use-calendar-month {:date (js/Date.)})]
    (! :row {:align :center
             :className "component"}
       (! :calendar/month
          {:days days}))))

(defnc calendar
  []
  (let [[value set-value!] (hooks/use-state (js/Date.))]
    (! :row {:align :center
             :className "component"}
       (! :calendar {:value value
                     :on-change set-value!}))))

(defnc calendar-period
  []
  (let [[value set-value!] (hooks/use-state nil)]
    (! :row {:align :center
             :className "component"}
       (! :calendar/period
          {:value value
           :on-change set-value!}))))

(defnc Calendar
  {:wrap [(router/wrap-rendered :toddler.calendar)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    (! :simplebar
       {:style {:height height
                :width width}}
       (! :row {:align :center}
          (! :column
             {:align :center
              :style {:max-width "30rem"}
              :className (css
                          ["& .component" :my-6])}
             ($ md/watch-url {:url "/calendar.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "calendar-month-example")}
                ($ month))
             ($ toddler/portal
                {:locator #(.getElementById js/document "calendar-example")}
                ($ calendar))
             ($ toddler/portal
                {:locator #(.getElementById js/document "calendar-period-example")}
                ($ calendar-period)))))))
