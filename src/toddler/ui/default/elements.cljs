(ns toddler.ui.default.elements
  (:require
    [helix.core
     :refer [$ defnc]]
    [helix.dom :as d]
    [helix.styled-components :refer [defstyled]]
    [toddler.elements :as e]
    [toddler.ui.default.color :refer [color]]
    ["toddler-icons$default" :as icon]))


(defstyled buttons
  "div"
  {:display "flex"
   :button {:margin 0
            :border-radius 0}
   :button:first-of-type {:border-top-left-radius 4 :border-bottom-left-radius 4}
   :button:last-of-type {:border-top-right-radius 4 :border-bottom-right-radius 4}})

(defn button-colors
  [{:keys [context disabled]}]
  (let [context (if disabled :disabled context)]
    (case context
      ;;
      :positive
      {:color "white"
       :background-color (color :green/dark)
       :hover {:color "white"
               :background-color (color :green)}}
      ;;
      :negative
      {:color (color :red)
       :background-color (color :gray/light)
       :hover {:color "white"
               :background-color (color :red)}}
      ;;
      :fun
      {:color "white"
       :background-color (color :teal)
       :hover {:background-color (color :teal/saturated)}}
      ;;
      :fresh
      {:color (color :gray)
       :background-color (color :gray/light)
       :hover {:color "black"
               :background-color (color :yellow)}}
      ;;
      :stale
      {:color (color :gray)
       :background-color (color :gray/light)
       ::hover {:color "white"
                :background-color (color :gray/dark)}}
      ;;
      :disabled
      {:color "white"
       :background-color "#bbbbbb"
       :cursor "initial"}
      ;;
      {:color "white"
       :background-color "#5e82b8"
       :hover {:background-color (color :asphalt/dark)}})))


(defstyled button
  "button"
  {:border "2px solid transparent"
   :border-radius 2
   :padding "5px 18px"
   :max-height 30
   :min-width 80
   :font-size "12"
   :line-height "1.33"
   :text-align "center"
   :vertical-align "center"
   :transition "box-shadow .3s ease-in,background .3s ease-in"
   :cursor "pointer"
   :margin "3px 2px"
   ":hover" {:transition "background .3s ease-in"}
   ":focus" {:outline "none"}
   ":active" {:transform "translate(0px,2px)" :box-shadow "none"}}
  (fn [{:keys [disabled] :as props}]
    (let [{:keys [background-color color hover]}
          (button-colors props)]
      (cond->
        {:color color 
         :background-color background-color 
         ":hover" (assoc hover :box-shadow "0px 2px 4px 0px #aeaeae")}
        disabled (assoc :pointer-events "none")))))


(defstyled checkbox e/Checkbox
  {:cursor "pointer"
   :path {:cursor "pointer"}
   :transition "color .2s ease-in"
   :width 20
   :height 20
   :border-radius 3
   :border-color "transparent"
   :padding 0
   :display "flex"
   :justify-content "center"
   :outline "none"
   :align-items "center"
   ":active" {:border-color "transparent"}}
  (fn [{:keys [theme value disabled]}]
    (let [[c bc] (case (:name theme)
                   ["white" (case value 
                              true (color :green)
                              false (color :disabled)
                              (color :gray/light))])]
      (cond->
        {:color c
         :background-color bc}
        disabled (assoc :pointer-events "none")))))


(defstyled row e/Row
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :flex-grow "1"
   ".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "14"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}}
  e/--flex-position)


(defstyled column e/Column
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"
   ".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "14"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}
   :padding 3}
  e/--flex-position)


(def components
  {:row row
   :column column
   :checkbox checkbox
   :button button
   :buttons buttons})
