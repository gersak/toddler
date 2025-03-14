(ns toddler.chart-js.lazy
  (:require
   [helix.core :refer [defnc $]]
   [helix.hooks :as hooks]
   [toddler.lazy :as lazy]))

(lazy/load
 "chartjs"
 ::Chart toddler.chart-js/Chart)
