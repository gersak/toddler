(ns toddler.md.lazy
  (:require
   ["react" :as react]
   [shadow.loader]
   [toddler.md :as md]))

(def show
  (react/lazy
   (fn []
     (->
      (shadow.loader/load "markdown")
      (.then (fn [_] #js {:default toddler.md/show}))))))

(def fetch
  (react/lazy
   (fn []
     (->
      (shadow.loader/load "markdown")
      (.then (fn [_] #js {:default toddler.md/fetch}))))))

(def from-url
  (react/lazy
   (fn []
     (->
      (shadow.loader/load "markdown")
      (.then (fn [_] #js {:default toddler.md/from-url}))))))

(def watch-url
  (react/lazy
   (fn []
     (->
      (shadow.loader/load "markdown")
      (.then (fn [_] #js {:default toddler.md/watch-url}))))))
