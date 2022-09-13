(ns toddler.dev.context
  (:require
    [helix.core :refer [create-context]]))


(def ^:dynamic *components* (create-context))
(def ^:dynamic *render* (create-context))
(def ^:dynamic *set-componets* (create-context))
(def ^:dynamic *navbar* (create-context))
(def ^:dynamic *user* (create-context))
(def ^:dynamic *header* (create-context))
