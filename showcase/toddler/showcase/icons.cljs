(ns toddler.showcase.icons
  (:require
   [helix.core :refer [defnc]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 #:showcase.icons {:default "Icons"
                   :hr "Ikone"})

(defnc Icons []
  (d/div "hi"))
