(ns toddler.showcase
  {:shadow.css/include
   ["css/toddler.css"]}
  (:require
   ["react" :as react]
   ["react-dom/client" :refer [createRoot]]
   [helix.dom :as d]
   [taoensso.telemere :as t]
   [toddler.dev :as dev]
   [toddler.ui :refer [wrap-ui]]
   [toddler.ui.components :as default]
   [helix.core :refer [$ defnc provider]]
   [toddler.showcase.layout :refer [Layout]]
   [toddler.showcase.inputs :refer [Inputs]]
   [toddler.showcase.table :refer [Table TableGrid]]
   [toddler.showcase.calendar :refer [Calendar]]
   [toddler.showcase.popup :refer [Popup]]
   [toddler.showcase.i18n :refer [i18n]]
   [toddler.showcase.routing :refer [Routing]]
   [toddler.showcase.icons :refer [Icons]]
   [toddler.showcase.modal :refer [Modal]]
   [toddler.showcase.notifications :refer [Notifications]]
   [toddler.showcase.rationale :refer [Rationale]]
   [toddler.notifications :as notifications]
   [toddler.router :as router]
   [toddler.md.context :as md.context]
   toddler.i18n.common))

(.log js/console "Loaded showcase!")

(defonce root (atom nil))

(def routes
  [{:id :toddler.rationale
    :name "Rationale"
    :render Rationale
    :segment "rationale"
    :landing 10}
   {:id :toddler.inputs
    :name "Inputs"
    :render Inputs
    :segment "inputs"}
   {:id :toddler.table
    :name "Table"
    :render Table
    :segment "tables"}
   {:id :toddler.calendar
    :name "Calendar"
    :render Calendar
    :segment "calendar"}
   {:id :toddler.layout
    :name "Layout"
    :render Layout
    :segment "layout"}
   {:id :toddler.popup
    :name "Popup"
    :render Popup
    :segment "popup"}
   {:id :toddler.modal
    :name "Modal"
    :render Modal
    :segment "modal"}
   {:id :toddler.notifications
    :name "Notifications"
    :render Notifications
    :segment "notifications"}
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
  {:wrap [(wrap-ui default/components)]}
  []
  ($ router/LandingPage
     {:url "/"
      :enforce-access? false}
     ($ notifications/Store
        {:class notifications/$default}
        ($ dev/playground
           {:max-width 1000
            :components routes})))

  ;; TODO - Strict mode causes problems with popup window
  #_($ react/StrictMode
       ($ router/Provider
          ($ dev/playground {:components routes}))))

(defnc LoadShowcase
  []
  ($ router/Provider
     {:base "toddler"}
     (provider
      {:context md.context/refresh-period
       :value 0}
      (provider
       {:context md.context/base
        :value "https://raw.githubusercontent.com/gersak/toddler/refs/heads/prep/github-page/dev"}
       ($ Showcase)))))

(defn start! []
  (.log js/console "Starting Toddler showcase!")
  (t/set-min-level! :info)
  ; (t/set-min-level! :log "toddler.md" :debug)
  ; (t/set-min-level! :log "toddler.routing" :debug)
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.log js/console "Rendering playground")
    (.render ^js @root ($ LoadShowcase))))

(set! (.-start js/window) start!)
