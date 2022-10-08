(ns toddler.showcase.avatar
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   ; [helix.dom :as d]
   [toddler.ui :as ui]
   [toddler.ui.provider :refer [UI]]
   [toddler.ui.default :as default]
   [toddler.elements.avatar :as a]
   [toddler.dev :as dev]))



(defnc Editor 
  []
  ($ UI
     {:components default/components}
     ($ a/Editor)))


(dev/add-component
   {:key ::editor
    :name "Avatar Editor"
    :render Editor})
