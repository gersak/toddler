(ns toddler.md.lazy
  (:require
   [helix.core :refer [defnc $ provider fnc]]
   [helix.hooks :as hooks]
   [toddler.lazy :as lazy]
   [toddler.md.context :as md.context]))

(lazy/load
 "markdown"
 ::show toddler.md/show
 ::from-url toddler.md/from-url
 ::watch-url toddler.md/watch-url
 ::img toddler.md/img)

(defn wrap-base [component base]
  (fnc MD [props]
    (provider
     {:context md.context/base
      :value base}
     ($ component {& props}))))

(defn wrap-refresh [component period]
  (fnc MD [props]
    (provider
     {:context md.context/refresh-period
      :value period}
     ($ component {& props}))))

(defn wrap-show [component md-props]
  (fnc MD [props]
    (provider
     {:context md.context/show
      :value md-props}
     ($ component {& props}))))


