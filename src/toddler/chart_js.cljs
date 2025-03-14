(ns toddler.chart-js
  (:require
   ["chart.js/auto" :as chart]
   [cljs-bean.core :refer [->js]]
   [toddler.ui :as ui]
   [helix.core :refer [defnc defhook $ create-context provider]]
   [helix.hooks :as hooks]
   [helix.dom :as d]))

(defnc Chart
  {:wrap [(ui/forward-ref)]}
  [{:keys [config]} _ref]
  (let [_local (hooks/use-ref nil)
        _canvas (or _ref _local)]
    (hooks/use-effect
      :once
      (when @_canvas
        (chart/Chart. @_canvas (->js config))))
    (d/canvas
     {:ref #(reset! _canvas %)})))
