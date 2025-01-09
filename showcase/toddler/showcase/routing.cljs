(ns toddler.showcase.routing
  (:require
   [helix.core :refer [defnc]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 #:showcase.routing {:default "Routing"
                     :hr "Usmjeravanje"})

(defnc Routing []
  (d/div "hi"))
