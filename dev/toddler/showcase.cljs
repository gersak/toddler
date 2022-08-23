(ns toddler.showcase
  (:require
    ["react-dom/client" :refer [createRoot]]
    [helix.core :refer [$ defnc <>]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [toddler.dev :as dev]
    [toddler.interactions :as interactions]))


(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render @root ($ dev/playground))))


(defnc AutosizeInput
  []
  (let [[state set-state!] (hooks/use-state "")]
    (letfn [(on-change [e]
              (set-state! (.. e -target -value)))]
      ($ dev/centered-component
         ($ interactions/input-field
            {:name "user name"
             :value state
             :onChange on-change})))))


(dev/add-component
  {:key ::autosize-input
   :name "AutosizeInput"
   :render AutosizeInput})


(defnc AvatarImage
  []
  (let [[state set-state] (hooks/use-state 100)]
    (<>
      ($ interactions/slider
         {:width "300px"
          :min "10"
          :max "500"
          :value (str state)
          :onChange (fn [e] (set-state (.-value (.-target e))))})
      (d/br)
      ($ interactions/avatar
         {:size (int state)
          :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/1920px-Image_created_with_a_mobile_phone.png"}))))


(dev/add-component
  {:key ::avatar-image
   :name "Avatar image"
   :render AvatarImage})


(defnc TimestampCalendar
  []
  (d/div
    {:style {:margin "auto",
             :width "20%"}}
    (let []
      ($ interactions/timestamp-calendar
         {:onChange (fn [x] (.log js/console "clicked day"))}))))



(dev/add-component
  {:key ::timestamp-calendar
   :name "Timestamp Calendar"
   :render TimestampCalendar})
