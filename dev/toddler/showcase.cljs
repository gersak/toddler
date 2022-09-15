(ns toddler.showcase
  (:require
   ["react-dom/client" :refer [createRoot]]
   [toddler.dev :as dev]
   [helix.core :refer [$]]
   toddler.showcase.inputs
   toddler.showcase.table))


(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render @root ($ dev/playground))))



