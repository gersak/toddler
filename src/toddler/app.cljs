(ns toddler.app
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [create-context]]))


(def ^:dynamic *user* (create-context))
(def ^:dynamic *subscription* (create-context))
(def ^:dynamic *window* (create-context))
(def ^:dynamic *layout* (create-context))
(def ^:dynamic *box* (create-context))


(defonce signal-channel (async/chan 100))
(defonce signal-publisher (async/pub signal-channel :topic))


