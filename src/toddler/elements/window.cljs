(ns toddler.elements.window
  (:require
   [clojure.core.async :as async]
   [vura.core :refer [round-number]]
   [toddler.app :as app]
   [toddler.hooks :refer [make-idle-service]]
   [helix.core :refer [defnc provider]]
   [helix.hooks :as hooks]
   [helix.children :as c]))


(defn- get-window-dimensions
  []
  {:width (round-number (..  js/window -visualViewport -width) 1 :floor)
   :height (round-number (.. js/window -visualViewport -height) 1 :floor)})

(defnc DimensionsProvider
  [props]
  (let [[state set-state!] (hooks/use-state (get-window-dimensions))
        resize-idle-service (hooks/use-ref
                             (make-idle-service
                              600
                              #(set-state! (get-window-dimensions))))]
    (hooks/use-effect
     [state]
     (async/go
       (async/<! (async/timeout 300))
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
