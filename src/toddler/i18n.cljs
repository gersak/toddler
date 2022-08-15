(ns toddler.i18n
  (:require 
    [toddler.i18n.dictionary 
     :refer [dictionary
             calendar]]
    toddler.i18n.en
    toddler.i18n.hr
    [tongue.core :as tongue]))


(defonce translator (atom nil))

(defn get-in-calendar [& keys]
  (get-in @calendar keys))

(defn add-watcher
  []
  (add-watch 
    dictionary nil
    (fn [_ _ _ new-state]
      (println "Renewing dictionary")
      (reset! translator (tongue/build-translate new-state))))
  (println "Added dictionary watcher...")
  true)

(defonce initialized?
  (do
    (add-watcher)
    (reset! translator (tongue/build-translate @dictionary))))
