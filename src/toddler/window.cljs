(ns toddler.window
  (:require
   [clojure.core.async :as async]
   [vura.core :refer [round-number]]
   [toddler.app :as app]
   [toddler.hooks :refer [make-idle-service]]
   [helix.core :refer [defnc provider fnc $]]
   [helix.hooks :as hooks]
   [helix.children :as c]))


(defn get-window-dimensions
  []
  (let [w (round-number (..  js/window -visualViewport -width) 1 :floor)
        h (round-number (.. js/window -visualViewport -height) 1 :floor)]
    {:x 0
     :y 0
     :top 0
     :bottom h
     :left 0
     :right w
     :width w
     :height h}))

(defnc DimensionsProvider
  [props]
  (let [[state set-state!] (hooks/use-state (get-window-dimensions))
        resize-idle-service (hooks/use-ref
                              (make-idle-service
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
         :context app/*window*}
        (c/children props)))))


(defn dimension-provider
  ([component]
   (fnc dimensions-provider [props]
     ($ DimensionsProvider ($ component {& props}))))
  ([component cprops]
   (fnc dimensions-provider[props]
     ($ DimensionsProvider {& cprops} ($ component {& props})))))
