;; Copyright (C) Neyho, Inc - All Rights Reserved
;; Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <robi@neyho.com>, June 2019


(ns toddler.elements.search
  (:require 
    ["react-icons/fa" :refer [FaSearch]]
    clojure.string
    [helix.core
     :refer [defnc $]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [toddler.hooks
     :refer [use-idle]]
    [toddler.elements.input
     :refer [AutosizeInput]]))


(defnc Search
  [{:keys [value icon on-change idle-timeout className onChange]
    :or {idle-timeout 500
         value ""
         icon FaSearch
         onChange identity}
    :as props}]
  (let [on-change (or on-change onChange identity)
        [input set-input!] (use-idle "" #(on-change %) idle-timeout)]
    (hooks/use-effect
      [value]
      (when (not= value input)
        (set-input! value)))
    (d/div
      {:className className}
      (d/div
        {:class "value"}
        ($ AutosizeInput
           {& (merge
                (dissoc props :className)
                {:value input
                 :on-change (fn [e] (set-input! (.. e -target -value)))})}))
      (d/div
        {:class "icon"} 
        ($ icon)))))
