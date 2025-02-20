(ns toddler.md.lazy
  (:require
   [helix.core :refer [$ defnc defhook fnc provider]]
   [helix.hooks :as hooks]
   [toddler.md.context :as md.context]
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
                     ::watch-url toddler.md/watch-url
                     ::img toddler.md/img))))))

(defnc not-found [])

(defhook use-function
  [k]
  (let [[f f!] (hooks/use-state (get @module k))]
    (hooks/use-effect
      :once
      (load-markdown))
    (hooks/use-effect
      :once
      (when-not f
        (let [w (gensym "md_loaded")]
          (add-watch module w
                     (fn [_ _ _ {f k}]
                       (f! f)))
          (fn []
            (remove-watch module w)))))
    (or f not-found)))

(defnc show
  [props]
  ($ (use-function ::show) {:& props}))

(defnc from-url
  [props]
  ($ (use-function ::from-url) {:& props}))

(defnc watch-url
  [props]
  ($ (use-function ::watch-url) {:& props}))

(defnc img
  [props _]
  ($ (use-function ::img) {:& props}))

(defn wrap-base [component base]
  (fnc MD [props]
    (provider
     {:context md.context/base
      :value base}
     ($ component {& props}))))

(defn wrap-refresh [component period]
  (fnc MD [props]
    (provider
     {:context md.context/refresh-period
      :value period}
     ($ component {& props}))))

(defn wrap-show [component md-props]
  (fnc MD [props]
    (provider
     {:context md.context/show
      :value md-props}
     ($ component {& props}))))
