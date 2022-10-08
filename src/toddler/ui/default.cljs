(ns toddler.ui.default
  (:require
    [helix.core :refer [$ defnc]]
    [toddler.head :as head]
    [toddler.ui.default.fields :as fields]
    [toddler.ui.default.elements :as elements]
    [toddler.ui.provider :as ui.provider]))


(def components
  (merge
    fields/components
    elements/components))


(defnc Provider [props]
  ($ ui.provider/UI
    {:components components
     & props}))


(head/add
  :link
  {:href "https://fonts.googleapis.com/css2?family=Roboto&display=swap"
   :rel "stylesheet"})
