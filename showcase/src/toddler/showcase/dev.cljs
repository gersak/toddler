(ns toddler.showcase.dev
  {:shadow.css/include
   ["css/toddler.css"]}
  (:require
   ["react-dom/client" :refer [createRoot]]
   [helix.core :refer [$ defnc provider]]
   [taoensso.telemere :as t]
   [toddler.router :as router]
   [toddler.showcase :refer [Showcase]]
   [toddler.md.context :as md.context]))

(defonce root (atom nil))

(defnc LoadShowcase
  []
  ($ router/Provider
     {:base ""}
     (provider
      {:context md.context/refresh-period
       :value 3000}
      (provider
       {:context md.context/base
        :value ""}
       ($ Showcase)))))

(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  ; (t/set-min-level! :debug)
  ; (t/set-min-level! :log "toddler.md" :debug)
  ; (t/set-min-level! :log "toddler.routing" :debug)
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (.log js/console "Rendering playground")
      (reset! root ^js (createRoot target)))
    (.render ^js @root ($ LoadShowcase))))
