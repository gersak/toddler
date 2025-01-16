(ns toddler.icons
  (:require
   [helix.core :refer [defnc $]]
   [helix.dom :as d]
   [toddler.fav6.solid :as fav6]
   [toddler.material.outlined :as material]
   ["react-dom/client" :refer [createRoot]]))

(.log js/console "Loaded icons!")

(defonce root (atom nil))

(defnc Icons
  []
  (d/div
   ($ fav6/ticket
      {:style {:height 20
               :width 20}})
   "HI")
  (d/div
   ($ material/lightbulb-circle
      {:style {:height 48
               :width 48
               :color "yellow"}})
   "HI"))

(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ Icons))))
