(ns toddler.showcase
  (:require
    ["react-dom/client" :refer [createRoot]]
    [helix.core :refer [$ defnc]]
    [helix.hooks :as hooks]
    [toddler.dev :as dev]
    [toddler.interactions :as interactions]))


(defonce root (atom nil))

(defn ^:dev/after-load start! []
  (let [target ^js (.getElementById js/document "app")]
    (when-not @root
      (reset! root ^js (createRoot target)))
    (.render @root ($ dev/playground))))


(defnc AutosizeInput
  []
  (let [[state set-state!] (hooks/use-state "")]
    (letfn [(on-change [e]
              (set-state! (.. e -target -value)))]
      ($ dev/centered-component
         ($ interactions/input-field
            {:name "user name"
             :value state
             :onChange on-change})))))


(dev/add-component
  {:key ::autosize-input
   :name "AutosizeInput"
   :render AutosizeInput})
