(ns toddler.showcase
  (:require
   ["react-dom/client" :refer [createRoot]]
   [toddler.dev :as dev]
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   ; toddler.showcase.calendar
   [toddler.showcase.fields :refer [Fields]]
   [toddler.showcase.table :refer [Table TableGrid]]
   ; toddler.showcase.avatar
   [toddler.router :as router]
   ))


(.log js/console "Loaded showcase!")


(defonce root (atom nil))


(def components
  [{:id :toddler.fields
    :name :showcase.fields
    :render Fields
    :segment "fields"}
   {:id :toddler.table 
    :name :showcase.tables
    :render Table
    :segment "tables"}
   {:id :toddler.multi-table 
    :name :showcase.multi-tables
    :render TableGrid
    :segment "multi-tables"}])


(defnc Showcase
  []
  ($ router/Provider
     ($ dev/playground {:components components})))


(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ Showcase))))
