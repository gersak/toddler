(ns toddler.layout
  (:require
    [helix.core
     :refer [defnc create-context
             defhook provider memo
             fnc $]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.children :as c]
    [vura.core :as vura]
    [toddler.ui :refer [forward-ref]]
    [toddler.app :as app]
    [toddler.hooks
     :refer [use-dimensions]]))


(defhook use-layout
  ([] (hooks/use-context app/*layout*))
  ([k] (get (hooks/use-context app/*layout*) k)))


(def ^:dynamic ^js *container* (create-context nil))
(def ^:dynamic ^js *container-dimensions* (create-context nil))
(def ^:dynamic ^js *container-style* (create-context nil))


(defhook use-container [] (hooks/use-context *container*))
(defhook use-container-dimensions [] (hooks/use-context *container-dimensions*))


(letfn [(same? [a b]
          (let [ks [:style :className]
                before (select-keys a ks)
                after (select-keys b ks)
                result (= before after)]
            result))]
  (defnc Container
    [{:keys [className style] :as props}]
    ;; TODO - maybe remove this
    ; {:wrap [(memo same?)]}
    (let [[container dimensions] (use-dimensions)]
      (d/div
        {:ref #(reset! container %)
         :className className
         :style style}
        (provider
          {:context *container-dimensions*
           :value dimensions}
          (provider
            {:context *container*
             :value container}
            (c/children props)))))))


(defn wrap-container
  ([component]
   (fnc Container [props]
     ($ Container ($ component {& props}))))
  ([component cprops]
   (fnc [props]
     ($ Container {& cprops} ($ component {& props})))))


(defn get-window-dimensions
  []
  (let [w (vura/round-number (..  js/window -visualViewport -width) 1 :floor)
        h (vura/round-number (.. js/window -visualViewport -height) 1 :floor)]
    {:x 0
     :y 0
     :top 0
     :bottom h
     :left 0
     :right w
     :width w
     :height h}))


; (defnc Column
;   [{:keys [label className position] :as props} _ref]
;   {:wrap [(forward-ref)]}
;   (d/div
;     {:ref _ref
;      :className className
;      :position position}
;     (when label
;       (d/div
;         {:className "label"}
;         (d/label label)))
;     (c/children props)))


; (defnc Row
;   [{:keys [label className position] :as props} _ref]
;   {:wrap [(forward-ref)]}
;   (d/div
;     {:ref _ref
;      :className className
;      :position position}
;     (when label
;       (d/div
;         {:className "label"}
;         (d/label label)))
;     (c/children props)))
