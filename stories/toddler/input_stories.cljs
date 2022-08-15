(ns toddler.input-stories
  (:require
    [helix.core :refer [$]]
    [helix.hooks :as hooks]
    [toddler.elements.input :as i]
    [toddler.interactions :as interactions]))



(def ^:export default
  #js {:title "Toddler Calendar"})

(defn ^:export AutosizeInput
  []
  (let [[state set-state!] (hooks/use-state "")]
    ($ interactions/simplebar
       {:width 800}
       ($ i/AutosizeInput
          {:value state
           :placeholder "hello"
           :onChange (fn [e] (set-state! (.. e -target -value)))}))))
