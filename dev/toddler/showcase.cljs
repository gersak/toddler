(ns toddler.showcase
  (:require
    ["react-dom/client" :refer [createRoot]]
    [helix.core :refer [$ defnc]]
    [helix.hooks :as hooks]
    [toddler.dev
     :refer [playground
             add-component]]
    [toddler.interactions :as interactions]))


(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render @root ($ playground))))


(defnc AutosizeInput
  []
  (let [[state set-state!] (hooks/use-state "")]
    ($ interactions/autosize-input
       {:placeholder "Write some text..."
        :value state
        :onChange (fn [e]
                    (set-state! (.. e -target -value)))})))


(add-component
  {:key ::autosize-input
   :name "AutosizeInput"
   :render AutosizeInput})
