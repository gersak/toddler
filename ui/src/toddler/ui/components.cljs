(ns toddler.ui.components
  (:require
   [helix.core :refer [$ defnc]]
    ; [toddler.head :as head]
   [toddler.ui :refer [UI]]
   [toddler.ui.fields :as fields]
   [toddler.ui.elements :as elements]
   [toddler.ui.tables :as tables]
   [toddler.ui.elements.modal :as modal]
   [toddler.ui.elements.calendar :as calendar]))

(def components
  (merge
   elements/components
   modal/components
   fields/components
   tables/components
   calendar/components))

(defnc Provider [props]
  ($ UI
     {:components components
      & props}))
