(ns toddler.ui.default
  (:require
    [helix.core :refer [$ defnc]]
    [toddler.head :as head]
    [toddler.ui.default.fields :as fields]
    [toddler.ui.default.elements :as elements]
    [toddler.ui.provider :as ui.provider]
    [toddler.ui.default.table :as table]))


(def components
  (merge
    fields/components
    elements/components
    table/components))


(defnc Provider [props]
  ($ ui.provider/UI
    {:components components
     & props}))

