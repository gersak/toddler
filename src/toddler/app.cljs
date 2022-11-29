(ns toddler.app
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [create-context defhook]]
   [helix.hooks :as hooks]))


(def ^:dynamic ^js *user* (create-context))
(def ^:dynamic ^js *currency* (create-context))
(def ^:dynamic ^js *subscription* (create-context))
(def ^:dynamic ^js *window* (create-context))
(def ^:dynamic ^js *layout* (create-context))
(def ^:dynamic ^js *box* (create-context))
(.log js/console "Loading toddler.app")

(defhook use-layout
  ([] (hooks/use-context *layout*))
  ([k] (get (hooks/use-context *layout*) k)))

(defhook use-window [] (hooks/use-context *window*))
(defhook use-currency-options [] (hooks/use-context *currency*))


(defonce signal-channel (async/chan 100))
(defonce signal-publisher (async/pub signal-channel :topic))


