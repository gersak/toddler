(ns toddler.showcase
  {:shadow.css/include
   ["css/toddler.css"]}
  (:require
   ["react-dom/client" :refer [createRoot]]
   [taoensso.telemere :as t]
   [toddler.dev :as dev]
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   ; toddler.showcase.calendar
   [toddler.showcase.fields :refer [Fields]]
   [toddler.showcase.table :refer [Table TableGrid]]
   [toddler.showcase.layout :refer [Layout]]
   [toddler.showcase.modal :refer [Modal]]
   [toddler.showcase.i18n :refer [i18n]]
   [toddler.showcase.routing :refer [Routing]]
   ; [toddler.showcase.prosemirror :refer [ProseMirror]]
   toddler.i18n.common
   ; toddler.showcase.avatar
   [toddler.router :as router]))

(.log js/console "Loaded showcase!")

(defonce root (atom nil))

(def components
  [{:id :toddler.layout
    :name :showcase.layout
    :render Layout
    :segment "layout"}
   {:id :toddler.fields
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
    :segment "multi-tables"}
   {:id :toddler.modal
    :name :showcase.modal
    :render Modal
    :segment "modal"}
   {:id :toddler.routing
    :name :showcase.routing
    :render Routing
    :segment "routing"}
   {:id :toddler.i18n
    :name :showcase.i18n
    :render i18n
    :segment "i18n"}
   #_{:id :toddler.prosemirror
      :name :showcase.prosemirror
      :render ProseMirror
      :segment "prosemirror"}])

(defnc Showcase
  []
  ($ router/Provider
     ($ dev/playground {:components components})))

(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  (t/set-min-level! :info)
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ Showcase))))
