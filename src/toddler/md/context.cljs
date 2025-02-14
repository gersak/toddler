(ns toddler.md.context
  (:require
   [helix.core :refer [create-context]]))

(def base (create-context))
(def refresh-period (create-context 3000))
(def show (create-context))
