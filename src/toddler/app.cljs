(ns toddler.app
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [create-context defhook]]
   [helix.hooks :as hooks]))

(def url (create-context))
(def locale (create-context))
(def user (create-context))
(def token (create-context))
(def currency (create-context))
(def subscription (create-context))
(def window (create-context))
(def layout (create-context))
(def box (create-context))
(def avatars (create-context))
(def graphql-url (create-context))

(defhook use-window [] (hooks/use-context window))
(defhook use-currency-options [] (hooks/use-context currency))

(defonce signal-channel (async/chan 100))
(defonce signal-publisher (async/pub signal-channel :topic))

(defn listen-to-signal [topic handler]
  (let [c (async/chan 10)]
    (async/sub signal-publisher topic c)
    (async/go-loop []
      (let [v (async/<! c)]
        (when v
          (handler v)
          (recur))))))
