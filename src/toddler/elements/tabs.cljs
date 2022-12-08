;; Copyright (C) Neyho, Inc - All Rights Reserved
;; Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <robi@neyho.com>, June 2019


(ns toddler.elements.tabs
  (:require
    clojure.string
    [helix.styled-components :refer [defstyled --themed]]
    [toddler.hooks :refer [use-delayed]]
    [helix.dom :as d]
    [helix.core :refer [defnc $ defhook]]
    [helix.hooks :as hooks]))

(defhook use-slide-containers
  [options selected]
  (let [previous (use-delayed selected 500)
        idxp (.indexOf options previous)
        idxs (.indexOf options selected)
        direction (cond 
                    (< idxp idxs) :rtl
                    (> idxp idxs) :ltr
                    :else nil)]
    (mapv
      (fn [option]
        (cond
          (= previous selected)
          (when (= option selected)
            {:option option
             :display? true
             :direction #{:in direction}})
          (nil? previous)
          {:option option
           :display? (= option selected)
           :direction #{:in :rtl}}
          ;;
          (= option selected)
          {:option option
           :display? true
           :direction #{:in direction}}
          ;;
          (= option previous)
          {:option option
           :display? true
           :direction #{:out direction}}))
      options)))


(defstyled container "div"
  nil
  (fn [{:keys [theme]}]
    (case (:name theme))))

(defhook use-container-scroll-monitor
  ([refresh el] (use-container-scroll-monitor refresh el 10))
  ([refresh el threshold]
    (let [scroll-position (hooks/use-ref nil)
          cache (hooks/use-ref false)
          [shadow? set-shadow!] (hooks/use-state false)] 
      (hooks/use-effect
        [shadow?]
        (reset! cache shadow?))
      ; (hooks/use-layout-effect
      (hooks/use-effect
        refresh
        (when el
          (letfn [(get-scroll []
                    {:x (.-scrollLeft el)
                     :y (.-scrollTop el)})
                  (scroll-handler []
                    (let [{:keys [y] :as position} (get-scroll)]
                      (reset! scroll-position position)
                      (cond
                        (and (> y threshold) (not @cache))
                        (set-shadow! true)
                        ;;
                        (and (<= y threshold) @cache)
                        (set-shadow! false)
                        :else nil)))] 
            (scroll-handler)
            (.addEventListener el "wheel" scroll-handler)
            #(.removeEventListener el "wheel" scroll-handler))))
      {:shadow? shadow? 
       :position @scroll-position})))

(defnc SlideContainer
  [{:keys [children width height direction display? style]}]
  (when display? 
    (d/div
      {:key :container
       :style (merge
                {:top 0 :left 0 :position "absolute"
                 :height height :width width}
                style)
       :class (case direction
                #{:rtl :in}
                "animated slideInRight faster"
                ;;
                #{:rtl :out}
                "animated slideOutLeft faster"
                ;;
                #{:ltr :in}
                "animated slideInLeft faster"
                ;;
                #{:ltr :out}
                "animated slideOutRight faster"
                nil)}
      children)))

(defstyled tab-container
  "div"
  {:display "flex"
   :justify-content "flex-start"
   :align-items "center"
   :flex-direction "row"
   :box-shadow "0px 2px 4px -4px black"
   " .tab" 
   {:font-size "1em"
    :font-weight "500"
    :margin "4px 0px"
    :padding "0px 7px"
    :transition "color .3s ease-in"
    :text-transform "capitalize"
    :user-select "none"
    "&:hover:not(.selected)" {:cursor "pointer"}}}
  --themed)

(defnc Tab 
  [{:keys [on-change selected]
    tab-name :name
    :or {on-change identity}}]
  (d/div 
    {:class (cond-> ["tab"]
              selected (conj "selected"))
     :on-click #(on-change tab-name)}
    (when tab-name
      (clojure.string/replace
        (name tab-name)
        #"[-_]+" " "))))


(defnc Tabs [{:keys [tabs selected onChange on-change className]
              :or {onChange identity
                   on-change (or onChange identity)}}]
  (d/div {:className className}
    (map 
      (fn [tab] 
        ($ Tab 
          {:key tab
           :name tab
           :on-change on-change
           :selected (= tab selected)}))
      tabs)))
