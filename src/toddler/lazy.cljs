(ns toddler.lazy
  (:require-macros [toddler.lazy :refer [load-components]])
  (:require
   ["react" :as react]
   [clojure.core.async :as async]
   [goog.object]
   [helix.core :refer [defnc defhook]]
   [helix.hooks :as hooks]
   [shadow.loader]
   [shadow.lazy]
   [shadow.cljs.modern]))

(defonce _loaded (js/Date.now))

(def tank (atom nil))

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

(defhook use-lazy
  [k]
  (let [[f f!] (hooks/use-state (get @tank k))]
    (hooks/use-effect
      :once
      (when-not f
        (let [w (gensym "lazy_")]
          (add-watch tank w
                     (fn [_ _ _ {_f k :as data}]
                       (when-not (= f _f)
                         (.log js/console "Registerd new component: " k _f)
                         (f! _f)
                         (.log js/console "AT THAT THING"))))
          (fn [] (remove-watch tank w)))))
    (when-not f (.warn js/console (str "[toddler.lazy] Component not loaded " k)))
    (if f
      (do
        (.log js/console "USING LAZY COMPONENT: " f)
        #_f
        not-found)
      (do
        (.warn js/console (str "[toddler.lazy] Component not loaded " k))
        not-found))))
