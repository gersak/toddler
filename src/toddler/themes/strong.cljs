(ns toddler.dev.strong-theme
  (:require
   [helix.styled-components :refer [--themed]]
   [toddler.theme :as theme]
   [toddler.head :as head]))


(def navbar-background "#061c30")

(def colors 
  (merge
    #:navbar.color {:default theme/gray}
    #:navbar.background{:default "#d3e9eb"
                        :strong "#061c30"}))


(defmethod --themed [:strong 'toddler.dev/navbar]
  [_]
  {:background (:navbar/strong colors)
   ; Adding border (even with box-sizing border-box) renders scrollbar when not needed
   ;:border-right "1px solid #a2ced2"
   :color theme/gray
   ".selected"
   {".icon" {:color theme/gray}
    ".name" {:color theme/dark-gray
             :font-weight "600"}}
   ".name:hover" {:color theme/dark-gray
                  :font-weight "600"
                  :text-decoration "none"}})

(defmethod --themed [:strong 'toddler.dev/header]
  [_]
  {".circular-button" {:font-family "Roboto"
                       :font-weight "bold"
                       :font-size "12px"
                       :border "2px solid #d3d3d3"
                       :color "#003366"
                       :width "40px"
                       :height "80%"
                       :background-color "#d3d3d3"
                       :text-align "center"
                       :border-radius "50%"
                       :transform "translateY(5px)"
                       :cursor "pointer"
                       :transition "all .2s ease"
                       ":hover" {:background-color "#dfdfdf"
                                 :border "2px solid #dfdfdf"}
                       ":active" {:transform "translateY(6px)"
                                  :background-color "#003366"
                                  :color "#d3d3d3"
                                  :border "2px solid #003366"}}
   ".circular-button-outlined" {:font-family "Roboto"
                                :font-weight "bold"
                                :font-size "12px"
                                :border "2px solid #d3d3d3"
                                :color "#003366"
                                :width "40px"
                                :height "40px"
                                :background-color "#d3d3d3"
                                :text-align "center"
                                :border-radius "50%"
                                :transform "translateY(5px)"
                                :cursor "pointer"
                                :transition "all .2s ease"
                                ":hover" {:border "2px solid #003366"}
                                ":active" {:transform "translateY(6px)"
                                           :background-color "#003366"
                                           :color "#d3d3d3"
                                           :border "2px solid #003366"}}})



(head/add
 :link
 {:href "https://fonts.googleapis.com/css2?family=Audiowide&display=swap"
  :rel "stylesheet"})
