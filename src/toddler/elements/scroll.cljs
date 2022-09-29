(ns toddler.elements.scroll
  (:require
    ["simplebar-react" :as SimpleBar]))


(def SimpleBar
  (if (= js/undefined SimpleBar/default)
    SimpleBar
    SimpleBar/default))
