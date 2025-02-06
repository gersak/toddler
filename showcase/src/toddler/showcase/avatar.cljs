(ns toddler.showcase.avatar
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.ui.components :as components]
   [toddler.avatar :as a]
   [toddler.layout :as layout]
   [toddler.dev :as dev]))


(defnc Editor 
  []
  ($ components/Provider
     ($ a/Editor)))


(dev/add-component
   {:key ::editor
    :name "Avatar Editor"
    :render Editor})


(defnc Generator
  []
  (let [palette ["#FB8B24"
                 "#EA4746"
                 "#E22557"
                 "#AE0366"
                 "#820263"
                 "#560D42"
                 "#175F4C"
                 "#04A777"
                 "#46A6C9"
                 "#385F75"
                 "#313B4B"
                 "#613C69"
                 "#913D86"
                 "#F03FC1"]
        [color set-color!] (hooks/use-state (rand-nth palette))]
     ($ components/Provider
        ($ a/GeneratorStage
           {:color color 
            :className (css
                          :mt-5
                          :justify-center
                          :flex)
            :background "white"}
           ($ ui/row
              {:className (css :mt-5 :flex :justify-center)}
              ($ ui/button
                 {:onClick (fn [] (set-color! (rand-nth palette)))}
                 "Generate"))))))


(dev/add-component
   {:key ::generator
    :name "Avatar Generator"
    :render Generator})


(defnc Avatars
   []
   (let [{:keys [width height]} (layout/use-container-dimensions)]
      ($ components/Provider
         ($ ui/simplebar
            {:style #js {:width width :height height}}
            ($ a/Generator
               {:className (css
                              {:top "0" :left "0"
                               :position :fixed
                               :visibility "hidden"})}
               (d/div
                  {:className (css :flex :flex-row :flex-wrap)}
                  (map
                     (fn [x] ($ a/Avatar {:key x :className (css {:width "144px"})}))
                     (range 100))))))))


(dev/add-component
   {:key ::avatar
    :name "Avatars"
    :render Avatars})
