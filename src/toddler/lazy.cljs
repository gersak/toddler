(ns toddler.lazy
  (:require-macros [toddler.lazy :refer [load-components]])
  (:require
   [goog.object]
   [shadow.loader]
   [shadow.lazy]
   [shadow.cljs.modern]))

(def tank (atom nil))
