(ns toddler.dev.context
  (:require
    [helix.core :refer [create-context]]))


(def ^:dynamic ^js *components* (create-context))
(def ^:dynamic ^js *render* (create-context))
(def ^:dynamic ^js *set-componets* (create-context))
(def ^:dynamic ^js *navbar* (create-context))
(def ^:dynamic ^js *user* (create-context))
(def ^:dynamic ^js *header* (create-context))
