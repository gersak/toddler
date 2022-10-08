(ns toddler.showcase.avatar
  (:require
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [helix.styled-components :refer [defstyled]]
   ; [helix.dom :as d]
   [toddler.ui :as ui]
   [toddler.ui.provider :refer [UI]]
   [toddler.ui.default :as default]
   [toddler.elements.avatar :as a]
   [toddler.dev :as dev]))



(defnc Editor 
  []
  ($ UI
     {:components default/components}
     ($ a/Editor)))


(dev/add-component
   {:key ::editor
    :name "Avatar Editor"
    :render Editor})



(defstyled generator-row ui/row
  {:margin-top 10
   :justify-content "center"
   :align-items "center"})

(defstyled generator-stage a/GeneratorStage
  {:margin-top 10
   :justify-content "center"
   :display "flex"})

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
    ($ UI
       {:components default/components}
       ($ generator-stage
          {:color color 
           :background "white"}
          ($ generator-row
             ($ ui/button
                {:onClick (fn [] (set-color! (rand-nth palette)))}
                "Generate"))))))

(dev/add-component
   {:key ::generator
    :name "Avatar Generator"
    :render Generator})


(defstyled avatar a/Avatar
   {:width 144})


(defstyled avatars-row ui/row
   {:display "flex"
    :flex-wrap "wrap"
    :flex-grow "1"})


(defnc Avatar
   []
   ($ UI
      {:components default/components}
      ($ a/Generator
         (map
            (fn [x] ($ avatar {:key x}))
            (range 100)))))

(dev/add-component
   {:key ::avatar
    :name "Avatar"
    :render Avatar})


