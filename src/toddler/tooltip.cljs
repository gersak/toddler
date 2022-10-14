(ns toddler.tooltip
  (:require
    clojure.string
    ["react" :as react]
    [helix.core :refer [defnc $]]
    [helix.dom :as d]
    [helix.children :as c]
    [helix.hooks :as hooks]
    [helix.styled-components :refer-macros [defstyled]]
    [toddler.hooks :refer [use-delayed]]
    [toddler.popup :as popup]))

(def basic
  {:display "flex"
   :justify-content "start"
   :flex-wrap "wrap"
   :border-radius 3
   :color "white"
   :background-color "rgba(0,0,0,.75)"
   :padding 7
   :box-shadow "0px 3px 10px -3px black"})

(def basic-content
  {:display "flex"
   :flex-direction "column"
   :justify-content "flex-start"
   :margin 3
   :font-size "12 !important"
   :font-weight "600 !important"})

(defnc ActionTooltip 
  [{:keys [message preference className disabled] 
    :or {preference popup/cross-preference}
    :as props} ref]
  {:wrap [(react/forwardRef)]}
  (let [[visible? set-visible!] (hooks/use-state nil)
        hidden? (use-delayed (not visible?) 300)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (if (and (some? message) (not disabled)) 
      ($ popup/Area
         {:ref area
          :className "popup_area"
          :onMouseLeave (fn [] (set-visible! false))
          :onMouseEnter (fn [] (set-visible! true))}
         (c/children props)
         (when visible?
           ($ popup/Element
              {:ref (or ref popup)
               :style {:visibility (if hidden? "hidden" "visible")
                       :animationDuration ".5s"
                       :animationDelay ".3s"}
               :preference preference
               :className (str className " animated fadeIn")}
              (d/div {:class "info-tooltip"} message))))
      (c/children props))))


(defstyled action-tooltip ActionTooltip
  (assoc basic ".info-tooltip" basic-content))


(comment
  (def els (array-seq (.getElementsByClassName js/document "tooltip-container")))
  (println els)
  (def el (first els))
  )
