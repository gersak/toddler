(ns toddler.md.lazy
  (:require
   ["react" :as react]
   [helix.core :refer [$ Suspense defnc]]
   [shadow.loader]))

(defnc show
  [props]
  ($ Suspense
     ($ (react/lazy
         (fn []
           (->
            (shadow.loader/load "markdown")
            (.then (fn [_] #js {:default toddler.md/show})))))
        {:& props})))

(defnc fetch
  [props]
  ($ Suspense
     ($ (react/lazy
         (fn []
           (->
            (shadow.loader/load "markdown")
            (.then (fn [_] #js {:default toddler.md/fetch})))))
        {:& props})))

(defnc from-url
  [props]
  ($ Suspense
     ($ (react/lazy
         (fn []
           (->
            (shadow.loader/load "markdown")
            (.then (fn [_] #js {:default toddler.md/from-url})))))
        {:& props})))

(defnc watch-url
  [props]
  ($ Suspense
     ($ (react/lazy
         (fn []
           (->
            (shadow.loader/load "markdown")
            (.then (fn [_] #js {:default toddler.md/watch-url})))))
        {:& props})))
