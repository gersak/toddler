(ns toddler.app
  "Namespace where app level context are defined."
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [create-context defhook]]
   [helix.hooks :as hooks]))

(def ^{:doc "Root URL of application"} url (create-context))
(def ^{:doc "User locale keyword :en, :en_US, de, zh_CN, :es ..."} locale (create-context))
(def ^{:doc "Theme name. Should be string"} theme (create-context))
(def ^{:doc "Context that will hold user information.
            Like first name, last name, roles, permissions"}
  user (create-context))
(def ^{:doc "Access token that frontend can use to talk to resource provider"} token (create-context))
; (def ^{:doc "What currencies are available: EUR, USD, GBP, CNY, JPY, etc."} currency (create-context))
(def ^{:doc "Will hold data about window dimensions `{:height x :width y}`"} window (create-context))
(def layout (create-context))
; (def box (create-context))
; (def avatars (create-context))
(def ^{:doc "GraphQL endpoint URL"} graphql-url (create-context))

(defhook use-window
  "Hook will return window dimensions. If window is resized,
  this context will be recomputed and propagated."
  []
  (hooks/use-context window))

; (defhook use-currency-options
;   "Returns currency context"
;   []
;   (hooks/use-context currency))

(defhook use-theme
  "Returns theme context value"
  []
  (hooks/use-context theme))

(defonce
 ^{:doc "Signal channel is channel that can be used to
         publish global app events. It is used by
         `toddler.core/use-toddler-publisher` hook to
         put events to signal-channel"}
 signal-channel (async/chan 100))

(defonce
 ^{:doc "Publisher channel is channel that can be used to
         listen global app events. It is used by
         `toddler.core/use-toddler-listener` hook to
         register to some topic and handle that events"}
 signal-publisher
  (async/pub signal-channel :topic))

(defn listen-to-signal
  "Function will register listener to global signal-channel
  topic and function that will handle those events.

  Returns channel that when closed will stop registered handler"
  [topic handler]
  (let [c (async/chan 10)]
    (async/sub signal-publisher topic c)
    (async/go-loop []
      (let [v (async/<! c)]
        (if-not v
          (async/unsub signal-publisher topic c)
          (do
            (handler v)
            (recur)))))
    c))
