(ns toddler.ui.components
  (:require
   [helix.core :refer [$ defnc]]
    ; [toddler.head :as head]
   [toddler.ui.fields :as fields]
   [toddler.ui.elements :as elements]
   [toddler.ui.tables :as tables]
   [toddler.ui.elements.modal :as modal]
   [toddler.provider :as provider]))

(def components
  (merge
   elements/components
   modal/components
   fields/components
   tables/components))

(defnc Provider [props]
  ($ provider/UI
     {:components components
      & props}))
