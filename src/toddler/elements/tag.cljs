;; Copyright (C) Neyho, Inc - All Rights Reserved
;; Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <robi@neyho.com>, June 2019


(ns toddler.elements.tag
  (:require
    [helix.dom :as d]
    [helix.core :refer [defnc $]]
    [helix.styled-components :refer [defstyled --themed]]
    ["/toddler/icons$default" :as icon]))


(defn --editable-tag [{:keys [editable?]}]
  (when-not editable?
    {:user-select "none"}))


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
        ($ icon/clear
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
