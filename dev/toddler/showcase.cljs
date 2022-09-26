(ns toddler.showcase)

; (ns toddler.showcase
;   #_(:require
;    ["react-dom/client" :refer [createRoot]]
;    [toddler.dev :as dev]
;    ; ["./icons" :as icon]
;    [helix.core :refer [$]]
;    toddler.showcase.inputs
;    toddler.showcase.table))


(.log js/console "Loaded showcase!")
(defn start! [] "hell")

; (.log js/console icon/FaTimes)

; (defonce root (atom nil))

; (defn ^:dev/after-load start! []
;   (.log js/console "Starting Toddler showcase!")
;   (let [target ^js (.getElementById js/document "app")]
;     (when-not @root
;       (reset! root ^js (createRoot target)))
;     (.log js/console "Rendering playground")
;     (.render @root ($ dev/playground))))
