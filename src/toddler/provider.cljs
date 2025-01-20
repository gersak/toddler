(ns toddler.provider
  (:require
   [helix.core
    :refer [provider defnc $ fnc]]
   [helix.children :as c]
   [helix.hooks :as hooks]
   [toddler.ui :as ui]))

(defnc UI
  "Provider of UI components. It will provide toddler.ui/__components__
  context that is expected to be map of keywords maped to component
  implementation.
  
  For list of target components check out toddler.ui namespace"
  [{:keys [components] :as props}]
  (provider
   {:context ui/__components__
    :value components}
   (c/children props)))

(defn wrap-ui
  "Wrapper for UI component. UI is Toddler ui components
  provider. Provider expects map with bound components to
  matching keys in toddler.ui namespace"
  ([component components]
   (fnc UI [props]
     ($ UI {:components components}
        ($ component {:& props})))))

(defnc ExtendUI
  "Component that is extending toddler.ui/__commponents__ context
  by merging components in props to existing value.

  New components will be available to children components"
  [{:keys [components] :as props}]
  (let [current (hooks/use-context ui/__components__)]
    (provider
     {:context ui/__components__
      :value (merge current components)}
     (c/children props))))

(defn extend-ui
  "Wrapper that will extend Toddler UI by merging new
  components onto currently available components."
  ([component components]
   (fnc ExtendUI [props]
     ($ ExtendUI {:components components}
        ($ component {:& props})))))
