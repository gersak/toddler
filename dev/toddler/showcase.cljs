(ns toddler.showcase
  (:require
   ["react-dom/client" :refer [createRoot]]
   [toddler.dev :as dev]
   [helix.core :refer [$]]
   toddler.showcase.inputs
   toddler.showcase.table))


(.log js/console "Loaded showcase!")

(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ dev/playground))))


(start!)
