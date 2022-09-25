(ns toddler.app
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [create-context]]))


(def ^:dynamic ^js *user* (create-context))
(def ^:dynamic ^js *subscription* (create-context))
(def ^:dynamic ^js *window* (create-context))
(def ^:dynamic ^js *layout* (create-context))
(def ^:dynamic ^js *box* (create-context))


(defonce signal-channel (async/chan 100))
(defonce signal-publisher (async/pub signal-channel :topic))


