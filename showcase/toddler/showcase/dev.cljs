(ns toddler.showcase.dev
  {:shadow.css/include
   ["css/toddler.css"]}
  (:require
   ["react-dom/client" :refer [createRoot]]
   [helix.core :refer [$ defnc provider]]
   [taoensso.telemere :as t]
   [toddler.showcase :refer [Showcase]]
   [toddler.md.context :as md.context]))

(defonce root (atom nil))

(defnc LoadShowcase
  []
  (provider
   {:context md.context/refresh-period
    :value 3000}
   (provider
    {:context md.context/base
     :value ""}
    ($ Showcase))))

(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  (t/set-min-level! :info)
  ; (t/set-min-level! :log "toddler.md" :debug)
  ; (t/set-min-level! :log "toddler.routing" :debug)
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ LoadShowcase))))
