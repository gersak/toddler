(ns toddler.elements.layout
  (:require
    [helix.core :refer [defnc create-context]]
    [helix.hooks :as hooks]
    [helix.dom :as d]))


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
    {:wrap [(memo same?)]}
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


(defnc Column
  [{:keys [label style className position] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (d/div
    {:ref _ref
     :className className
     :style (->js style)
     :position position}
    (when label
      (d/div
        {:className "label"}
        (d/label label)))
    (c/children props)))


(defstyled column Column
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"})


(defnc Row
  [{:keys [label className style position] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (d/div
    {:ref _ref
     :className className
     :style (->js style)
     :position position}
    (when label
      (d/div
        {:className "label"}
        (d/label label)))
    (c/children props)))


(defstyled row Row
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :flex-grow "1"})
