(ns toddler.scroll
  (:require
    ["simplebar-react" :as simplebar]
    [cljs-bean.core :refer [->js]]
    [helix.core :refer [defnc $]]
    [toddler.ui :refer [forward-ref]]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :refer [transform-keys]]))



(def _SimpleBar
  (if (== js/undefined simplebar/default)
    simplebar 
    simplebar/default))


(defnc SimpleBar
  [props _ref]
  {:wrap [(forward-ref)]}
  ($ _SimpleBar
    {:ref _ref
     & (update props :style (comp ->js #(transform-keys csk/->camelCaseString %)))}))