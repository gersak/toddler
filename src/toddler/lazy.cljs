(ns toddler.lazy
  (:require-macros [toddler.lazy :refer [load]])
  (:require
   ["react" :as react]
   [clojure.core.async :as async]
   [goog.object]
   [helix.core :refer [defnc defhook]]
   [helix.hooks :as hooks]
   [shadow.loader]
   [shadow.lazy :as lazy]
   [shadow.cljs.modern :refer [js-await]]))

(defonce _loaded (js/Date.now))

(defonce tank (atom nil))

(defn load*
  [file mapping]
  (letfn [(execute []
            (when (some
                   (fn [k] (nil? (get @tank k)))
                   (keys (mapping)))
              (->
               (shadow.loader/load file)
               (.then (fn [_]
                        (swap! tank merge (mapping)))))))
          (expired? [time]
            (> (- (js/Date.now) _loaded) time))]
    (async/go-loop
     []
      (if (expired? 100)
        (execute)
        (do
          (async/<! (async/timeout 10))
          (recur))))))

(defnc not-found [])

(defhook use-component
  [k]
  (let [[f f!] (hooks/use-state (get @tank k))]
    (hooks/use-effect
      :once
      (when-not f
        (let [w (gensym "lazy_")]
          (add-watch tank w
                     (fn [_ _ _ {_f k}]
                       (when-not (= f _f)
                         (f! _f))))
          (fn [] (remove-watch tank w)))))
    (or f not-found)))
