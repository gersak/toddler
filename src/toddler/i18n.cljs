(ns toddler.i18n
  (:require 
    [toddler.i18n.dictionary 
     :refer [dictionary
             calendar]]
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

(def ^:dynamic *locale* :en)


(defprotocol Translator
  (translate
    [this]
    [this locale]
    [this locale options] "Translates input data by using additional opitons"))


(defprotocol Locale
  (locale [this key] "Returns locale definition for given key"))


(comment
  (translate (js/Date.) :hr))
