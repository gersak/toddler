(ns toddler.tauri
  (:require
   [clojure.core.async :as async]
   [toddler.app :as app]
   [helix.core :refer [defnc provider fnc $]]
   [helix.hooks :as hooks]
   [helix.children :refer [children]]
   [vura.core :refer [round-number]]
   [toddler.core :as toddler]
   ["@tauri-apps/api/window" :as window]))

(defn get-window-dimensions
  []
  (let [[w h] window/appWindow
        w (round-number (.-innerWidth js/window) 1 :floor)
        h (round-number (.-innerHeight js/window) 1 :floor)]
    {:x 0
     :y 0
     :top 0
     :bottom h
     :left 0
     :right w
     :width w
     :height h}))

(defnc DimensionsProvider
  "Component will track windows dimensions and provide current
  window width and height through app/window context"
  [props]
  (let [[state set-state!] (hooks/use-state (get-window-dimensions))
        resize-idle-service (hooks/use-ref
                             (toddler/make-idle-service
                              10
                              #(set-state! (get-window-dimensions))))]
    (hooks/use-effect
      [state]
      (async/go
        (async/<! (async/timeout 30))
        (when (not= state (get-window-dimensions))
          (async/put! @resize-idle-service :resized))))
    (letfn [(track-window-size []
              (async/put! @resize-idle-service :resized))]
      (hooks/use-effect
        :once
        (.addEventListener js/window "resize" track-window-size)
        #(do
           (async/close! @resize-idle-service)
           (.removeEventListener js/window "resize" track-window-size)))
      (provider
       {:value state
        :context app/window}
       (children props)))))

(defn wrap-window-provider
  "Wraps DimensionsProvider around target component"
  ([component]
   (fnc dimensions-provider [props]
     ($ DimensionsProvider ($ component {& props}))))
  ([component cprops]
   (fnc dimensions-provider [props]
     ($ DimensionsProvider {& cprops} ($ component {& props})))))
