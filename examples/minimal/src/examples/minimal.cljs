(ns examples.minimal
  {:shadow.css/include
   ["examples/minimal.css"]}
  (:require
   ["react-dom/client" :refer [createRoot]]
   [helix.core :refer [$ defnc provider]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.app :as app]
   [toddler.ui :as ui]
   [toddler.i18n.time]
   [toddler.core :as toddler]
   [toddler.ui.components :refer [components]]
   [toddler.router :as router]))

(defonce root (atom nil))

(defn Translate
  []
  (let [translate (toddler/use-translate)]
    (d/div (translate (js/Date.)))))

(defnc Minimal
  {:wrap [(ui/wrap-ui components)
          (router/wrap-router)
          (router/wrap-landing "/" false)]}
  []
  (let [[locale set-locale!] (hooks/use-state :en)]
    (provider
     {:context app/locale
      :value locale}
     ($ ui/row
        {:align :center
         :style {:width "100%" :height "100%"}}
        ($ ui/column
           {:align :center
            :style {:align-items "center"}}
           ($ ui/row
              ($ Translate)
              (d/div {:style {:font-size "100px"}} "🖖")
              (d/div "Hello from minimal example")))))))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (println "HIII")
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render ^js @root ($ Minimal))))
