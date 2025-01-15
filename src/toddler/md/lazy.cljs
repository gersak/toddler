(ns toddler.md.lazy
  (:require
   ["react" :as react]
   [helix.core :refer [$ Suspense defnc]]
   [helix.hooks :as hooks]
   [shadow.loader]))

(defonce module (atom nil))

(defn load-markdown
  []
  (when (nil? @module)
    (->
     (shadow.loader/load "markdown")
     (.then (fn [_]
              (swap! module assoc
                     ::show toddler.md/show
                     ::from-url toddler.md/from-url
                     ::watch-url toddler.md/watch-url))))))

(defnc show
  [props]
  (hooks/use-effect
    :once
    (load-markdown))
  (when-some [comp (get @module ::show)]
    ($ comp {:& props})))

(defnc from-url
  [props]
  (hooks/use-effect
    :once
    (load-markdown))
  (when-some [comp (get @module ::from-url)]
    ($ comp {:& props})))

(defnc watch-url
  [props]
  (hooks/use-effect
    :once
    (load-markdown))
  (when-some [comp (get @module ::watch-url)]
    ($ comp {:& props})))
