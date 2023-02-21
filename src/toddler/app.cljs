(ns toddler.app
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [create-context provider defhook defnc]]
   [helix.hooks :as hooks]
   [helix.children :refer [children]]))

(def url (create-context))
(def user (create-context))
(def token (create-context))
(def currency (create-context))
(def subscription (create-context))
(def window (create-context))
(def layout (create-context))
(def box (create-context))
(def avatars (create-context))
(def graphql-url (create-context))
(.log js/console "Loading toddler.app")


(defhook use-layout
  ([] (hooks/use-context layout))
  ([k] (get (hooks/use-context layout) k)))


(defhook use-window [] (hooks/use-context window))
(defhook use-currency-options [] (hooks/use-context currency))


(defnc Avatars
  [props]
  (let [avatars_ (atom nil)]
    (provider
      {:context avatars
       :value avatars_}
      (children props))))


(defonce signal-channel (async/chan 100))
(defonce signal-publisher (async/pub signal-channel :topic))


(def local-storage 
  (try
    js/window.localStorage
    (catch js/Error _ nil)))


(defn listen-to-signal [topic handler]
  (let [c (async/chan 10)]
    (async/sub signal-publisher topic c)
    (async/go-loop []
      (let [v (async/<! c)]
        (when v
          (handler v)
          (recur))))))
