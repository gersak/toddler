(ns toddler.provider
  (:require
    [helix.core :refer [provider defnc]]
    [helix.children :as c]
    [helix.hooks :as hooks]
    [toddler.ui :as ui]))


(defnc UI
  [{:keys [components] :as props}]
  (provider
    {:context ui/__components__
     :value components}
    (c/children props)))


(defnc ExtendUI
  [{:keys [components] :as props}]
  (let [current (hooks/use-context ui/__components__)]
    (provider
      {:context ui/__components__
       :value (merge current components)}
      (c/children props))))
