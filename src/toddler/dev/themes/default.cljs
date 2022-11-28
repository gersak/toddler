(ns toddler.dev.themes.default
  (:require
    [helix.styled-components :refer [--themed]]
    [toddler.themes.default
     :refer [color]
     :rename {color default}]
    [toddler.head :as head]))


(def color
  (merge
    #:navbar {:background "#d3e9eb"
              :color "#2c2c2c"}))


(defmethod --themed [{} 'toddler.dev/navbar]
  [_]
  {:background (color :navbar/background) 
   :color (color :navbar/color)
   ".selected"
   {".icon" {:color (default :gray)}
    ".name" {:color (default :gray/dark)
             :font-weight "600"}}
   ".name:hover" {:color (default :gray/dark)
                  :font-weight "600"
                  :text-decoration "none"}})

(defmethod --themed [{} 'toddler.dev/header]
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
