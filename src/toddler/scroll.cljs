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

(defn transform-style
  [data]
  (->js (transform-keys csk/->camelCaseString data)))

(defnc SimpleBar
  {:wrap [(forward-ref)]}
  [props _ref]
  ($ _SimpleBar
     {& (cond-> (update props :style transform-style)
          _ref (assoc :scrollableNodeProps #js {:ref _ref}))}))
