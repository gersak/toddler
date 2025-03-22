(ns toddler.chart-js.lazy
  (:require
   [toddler.lazy :as lazy]))

(lazy/load-components
 ::Chart toddler.chart-js/Chart)
