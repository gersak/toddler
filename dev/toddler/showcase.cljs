(ns toddler.showcase
  (:require
    ["react-dom/client" :refer [createRoot]]
    [helix.core :refer [defnc $]]
    [helix.dom :as d]
    [toddler.dev :as dev]
    toddler.showcase.inputs
    toddler.showcase.table))


(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    #_(.render ^js @root ($ Hello))
    (.render ^js @root ($ dev/Playground))))

; (start!)
