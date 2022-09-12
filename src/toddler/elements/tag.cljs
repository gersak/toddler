;; Copyright (C) Neyho, Inc - All Rights Reserved
;; Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <robi@neyho.com>, June 2019


(ns toddler.elements.tag
  (:require
    [helix.dom :as d]
    [helix.core :refer [defnc $]]
    [helix.styled-components :refer [defstyled --themed]]
    ["react-icons/fa" :refer [FaTimes]]))


(defn --editable-tag [{:keys [editable?]}]
  (when-not editable?
    {:user-select "none"}))

; (defn --colored-tag
;   [{:keys [theme context]}]
;   (let [[c b rc rch]
;         (case theme
;           (case context
;             :positive ["white" default/green default/dark-green "black"]
;             :negative ["white" default/red default/dark-red "black"]
;             :fun ["white" default/saturated-teal default/dark-teal "black"]
;             :fresh ["black" default/yellow default/dark-yellow "black"]
;             :stale ["white" default/gray default/dark-gray "black"]
;             ["white" default/teal default/dark-teal default/red]))] 
;     {:color c
;      :background-color b
;      " .remove" {:color rc
;                  :cursor "pointer"
;                  :transition "color .2s ease-in"
;                  :path {:cursor "pointer"}}
;      " .remove:hover" {:color rch}}))


(defnc DefaultContent
  [{:keys [value className]}]
  (d/div
    {:className className}
    value))

(defnc Tag 
  [{:keys [value
           context
           on-remove
           onRemove
           disabled
           className
           render/content]
    :or {content DefaultContent}}]
  (let [on-remove (some #(when (fn? %) %) [onRemove on-remove])]
    (d/div
      {:context (if disabled :stale context)
       :className className}
      ($ content {:className "content" :value value})
      (when on-remove 
        ($ FaTimes
           {:className "remove"
            :pull "left"
            :size "xs"
            :onClick (fn [e] (.stopPropagation e) (on-remove value))})))))

(defstyled tag Tag
  {:margin 3
   :display "flex"
   :flex-direction "row"
   :justify-content "start"
   :align-items "center"
   :flex-wrap "wrap"
   " .content" 
   {:padding "5px 5px"
    :justify-content "center"
    :align-items "center"
    :font-size "14"
    :display "flex"}
   :svg {:margin "0 5px"
         :padding-right 3}
   :border-radius 3}
  --editable-tag
  --themed)
