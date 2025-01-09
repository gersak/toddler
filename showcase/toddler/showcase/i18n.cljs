(ns toddler.showcase.i18n
  (:require
   [helix.core :refer [defnc]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 #:showcase.i18n {:default "i18n"})

(defnc i18n []
  (d/div "hi"))
