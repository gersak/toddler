(ns toddler.ui.components
  (:require
    [helix.core :refer [$ defnc]]
    ; [toddler.head :as head]
    [toddler.ui.fields :as fields]
    [toddler.ui.elements :as elements]
    [toddler.ui.tables :as tables]
    [toddler.ui.provider :as ui.provider]))


(def components
  (merge
    fields/components
    elements/components
    tables/components))


(defnc Provider [props]
  ($ ui.provider/UI
    {:components components
     & props}))
