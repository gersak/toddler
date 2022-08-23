(ns toddler.dev.theme
  (:require
    [helix.styled-components :refer [--themed]]
    [toddler.theme :as theme]
    [toddler.head :as head]))


(defmethod --themed [{} 'toddler.dev/navbar]
  [_]
  {:background "#d3e9eb"
   :border-right "1px solid #a2ced2"
   :color theme/gray
   ".selected"
   {".icon" {:color theme/gray}
    ".name" {:color theme/dark-gray
             :font-weight "600"}}
   ".name:hover" {:color theme/dark-gray
                  :font-weight "600"
                  :text-decoration "none"}})



(head/add
  :link
  {:href "https://fonts.googleapis.com/css2?family=Audiowide&display=swap"
   :rel "stylesheet"})
