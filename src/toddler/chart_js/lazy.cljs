(ns toddler.chart-js.lazy
  (:require
   [toddler.lazy :as lazy]))

(lazy/load-components
 "chartjs"
 ::Chart toddler.chart-js/Chart)
