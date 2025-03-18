(ns toddler.chart-js
  (:require
   ["chart.js/auto" :as chart]
   [cljs-bean.core :refer [->js]]
   [toddler.ui :as ui]
   [helix.core :refer [defnc]]
   [helix.hooks :as hooks]
   [helix.dom :as d]))

(defnc Chart
  {:wrap [(ui/forward-ref)]}
  [{:keys [config]} _ref]
  (let [_local (hooks/use-ref nil)
        _canvas (or _ref _local)
        _instance (hooks/use-ref nil)]
    (hooks/use-effect
      [config]
      (when (and @_canvas config)
        (reset! _instance (chart/Chart. @_canvas (->js config))))
      (fn []
        (when-let [chart @_instance]
          (.destroy chart))))
    (d/canvas
     {:ref #(reset! _canvas %)})))
