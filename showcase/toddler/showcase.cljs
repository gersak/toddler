(ns toddler.showcase
  {:shadow.css/include
   ["css/toddler.css"]}
  (:require
   ["react" :as react]
   ["react-dom/client" :refer [createRoot]]
   [taoensso.telemere :as t]
   [toddler.dev :as dev]
   [toddler.provider :refer [UI]]
   [toddler.ui.components :as default]
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [toddler.showcase.fields :refer [Fields]]
   [toddler.showcase.table :refer [Table TableGrid]]
   [toddler.showcase.layout :refer [Layout]]
   [toddler.showcase.popup :refer [Popup]]
   [toddler.showcase.i18n :refer [i18n]]
   [toddler.showcase.routing :refer [Routing]]
   [toddler.showcase.icons :refer [Icons]]
   [toddler.notifications :as notifications]
   [toddler.showcase.prosemirror :refer [ProseMirror]]
   [toddler.router :as router]
   toddler.i18n.common))

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
   #_{:id :toddler.multi-table
      :name :showcase.multi-tables
      :render TableGrid
      :segment "multi-tables"}
   {:id :toddler.popup
    :name "Popup"
    :render Popup
    :segment "popup"}
   {:id :toddler.routing
    :name :showcase.routing
    :render Routing
    :segment "routing"}
   {:id :toddler.i18n
    :name :showcase.i18n
    :render i18n
    :segment "i18n"}
   {:id :toddler.icons
    :name :showcase.icons
    :render Icons
    :segment "icons"}
   #_{:id :toddler.prosemirror
      :name :showcase.prosemirror
      :render ProseMirror
      :segment "prosemirror"}])

(defnc Showcase
  []
  ($ router/Provider
     ($ UI
        {:components default/components}
        ($ notifications/Store
           {:class notifications/$default}
           ($ dev/playground
              {:max-width 1000
               :components components}))))
  ;; TODO - Strict mode causes problems with popup window
  #_($ react/StrictMode
       ($ router/Provider
          ($ dev/playground {:components components}))))

(defn ^:dev/after-load start! []
  (.log js/console "Starting Toddler showcase!")
  (t/set-min-level! :debug)
  ; (t/set-min-level! :log "toddler.md" :debug)
  (t/set-min-level! :log "toddler.routing" :debug)
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ Showcase))))
