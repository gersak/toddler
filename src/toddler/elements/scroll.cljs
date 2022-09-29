(ns toddler.elements.scroll
  (:require
    ["simplebar-react" :as simplebar]))



(def SimpleBar
  (if (== js/undefined simplebar/default)
    simplebar 
    simplebar/default))
