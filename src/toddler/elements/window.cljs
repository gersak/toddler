(ns toddler.elements.window
  (:require
   [clojure.core.async :as async]
   [toddler.app :as app]
   [toddler.hooks :refer [make-idle-service]]
   [helix.core :refer [defnc provider]]
   [helix.hooks :as hooks]
   [helix.children :as c]))


(defnc DimensionsProvider
  [props]
  (let [[state set-state!] (hooks/use-state
                            {:width (- (.-innerWidth js/window) 1)
                             :height (- (.-innerHeight js/window) 1)})
        resize-idle-service (hooks/use-ref
                             (make-idle-service
                              200
                              #(set-state!
                                {:width (- (.-innerWidth js/window) 1)
                                 :height (- (.-innerHeight js/window) 1)})))]
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
