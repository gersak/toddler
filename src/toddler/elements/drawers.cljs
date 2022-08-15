(ns toddler.elements.drawers
  (:require
   clojure.string
   [helix.styled-components :refer [defstyled --themed]]
   [toddler.elements.tooltip
    :refer [action-tooltip]]
   [toddler.interactions :as interactions]
   [helix.dom :as d]
   [helix.children :as c]
   [helix.core :refer [defnc $ memo]]))


(defnc DrawerAction
  [{:keys [tooltip
           className
           icon
           preference]
    :or {preference [#{:center :right}
                     #{:bottom :left}
                     #{:top :left}]}
    :as props}]
  (if (string? tooltip) 
    ($ action-tooltip
       {:preference preference
        :message tooltip}
       (d/button
         {:class className
          & (dissoc 
              props 
              :position-preference 
              :className :icon :tooltip :action)}
         ($ interactions/fa {:icon icon})))
    (d/button
      {:class className
       & (dissoc 
           props 
           :position-preference 
           :className :icon :tooltip :action)}
      ($ interactions/fa {:icon icon}))))

(defstyled drawer-action DrawerAction
  {:pointer-events "auto"
   :cursor "pointer"
   :display "flex"
   :justify-content "center"
   :align-items "center"
   :border-radius 25
   :transition "all 0.2s ease-in"
   :margin "2px 7px"
   :width 40 :height 40
   :outline "none"
   (str interactions/fa) {:height 16}}
  --themed)

(defnc VerticalDrawer
  [{:keys [selected
           position
           width
           height
           max-width
           actions
           className]
    :or {position {:top 0
                   :left 0}}
    :as props}]
  {:wrap [(memo =)]}
  (let [opened? (some #{selected} (map :action actions))]
    (d/div
     {:class (clojure.string/join
              " "
              (cond-> [className
                       (some
                         #(when (contains? position %) (name %))
                         [:right :left])]
                opened? (conj "opened")))
      :style (cond->
              (merge
               {:position "absolute"
                :width width
                :height height}
               position)
               opened? (assoc :width max-width))}
     (d/div
      {& (merge
          {:class "header"
           :position "absolute"}
          position)}
      (d/div
       {:class (clojure.string/join
                " "
                (cond-> ["actions"]
                  (contains? position :bottom)
                  (conj "bottom")
                  (contains? position :right)
                  (conj "right")
                  (contains? position :top)
                  (conj "top")
                  (contains? position :left)
                  (conj "left")))
        :style {:display "flex"
                :flex-direction "column"
                :align-items "center"}}
       (map
        (fn [{:keys [action onClick]
              :as action-props}]
          ($ drawer-action
             {:key (name action)
              :onClick onClick
              :selected (= selected action)
              :position-preference 
              (if (contains? position :left)
                [["right"]]
                [["left"]])
              & action-props}))
        actions)))
     (d/div
      {:class "content"
       :style {:width (if opened? (- max-width width) 0)}}
      (c/children props)))))

(defstyled vertical VerticalDrawer
  {:display "flex"
   :justify-content "row"
   :box-sizing "border-box"
   "&.opened" 
   {".content" {:transition "width .3s ease-in"}
    ".header" 
    {:padding "10px 0"
     ".actions" 
     {:display "flex"
      :justify-content "flex-start"
      :flex-direction "column"
      :align-items "center"}}}}
  (fn [{:keys [theme]}]
    (case (:name theme)
      {:background-color "white" 
       "&.opened" 
       {:background-color "#edffff" }})))
